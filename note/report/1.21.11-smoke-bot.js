const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const { Vec3 } = require('vec3');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const log = (...a) => console.log('[smoke]', ...a);
const HOST = '127.0.0.1';

function customName(it) { try { return it && it.customName ? JSON.stringify(it.customName) : ''; } catch { return ''; } }
function findByName(bot, zh) { return bot.inventory.items().find(i => customName(i).includes(zh)); }

async function main() {
  const rcon = await Rcon.connect({ host: HOST, port: 25575, password: 'sfaudit' });
  const rc = async cmd => { const r = await rcon.send(cmd); log('RCON>', cmd.slice(0, 60), '=>', (r || '').slice(0, 70).replace(/\n/g, ' ')); return r; };
  const bot = mineflayer.createBot({ host: HOST, port: 25565, username: 'AuditBot', version: '1.21.11' });
  bot.on('kicked', r => { log('KICKED', r); process.exit(1); });
  await new Promise(res => bot.once('spawn', res));
  await rc(`/time set noon`); await rc(`/tp AuditBot 0.5 102.5 0.5`); await sleep(800);
  await rc(`/fill -6 100 -6 8 104 8 minecraft:stone`); await rc(`/fill -6 101 -6 8 104 8 minecraft:air`); await sleep(600);
  // per-run layout reset (fill air already clears blocks; SF data cleared server-side by world edit? we rely on fresh fill)
  await rc(`/sf research AuditBot all`); await sleep(4000);
  for (const [id, n] of [['ENERGY_REGULATOR',1],['ENERGY_CONNECTOR',2],['SOLAR_GENERATOR',1],['ELECTRIC_GOLD_PAN',1],['CARGO_MANAGER',1],['CARGO_NODE_INPUT',8],['CARGO_NODE_OUTPUT',8]]) { await rc(`/sf give AuditBot ${id} ${n}`); await sleep(400); }
  await rc(`/give AuditBot minecraft:coal 16`); await rc(`/give AuditBot minecraft:chest 2`); await sleep(800);

  const Y = 101;
  async function place(zhName, x, y, z, ref, face) {
    const it = findByName(bot, zhName);
    if (!it) { log('MISSING', zhName); return false; }
    await bot.equip(it, 'hand'); await sleep(150);
    const before = bot.blockAt(new Vec3(x,y,z)).name;
    await rc(`/tp AuditBot ${x+0.5} ${y+1.0} ${z-1.5}`); await sleep(350);
    await bot.lookAt(new Vec3(ref.x+0.5, ref.y+0.5, ref.z+0.5)); await sleep(150);
    for (let attempt = 0; attempt < 3; attempt++) {
      try { await bot.placeBlock(bot.blockAt(ref), face); } catch (e) { /* tolerated */ }
      await sleep(900);
      if (bot.blockAt(new Vec3(x,y,z)).name !== 'air' && bot.blockAt(new Vec3(x,y,z)).name !== before) break;
      await bot.equip(findByName(bot, zhName) || it, 'hand'); await sleep(300);
      await rc(`/tp AuditBot ${x + (attempt % 2 ? -1.5 : 0.5)} ${y + 1.0} ${z - (attempt % 2 ? 0.5 : 1.5)}`); await sleep(350);
      await bot.lookAt(new Vec3(ref.x+0.5, ref.y+0.5, ref.z+0.5)); await sleep(200);
    }
    const after = bot.blockAt(new Vec3(x,y,z)).name;
    log('PLACE', zhName, before, '->', after);
    return after !== before && after !== 'air';
  }
  // ===== all placements against the stone floor (y=101 row), pan adjacent to connector
  // REG(0,0) conn(1,0) conn(2,0) solar(3,0) pan(2,1)
  await place('调节器', 0,Y,0, new Vec3(0,Y-1,0), new Vec3(0,1,0));
  await place('连接器', 1,Y,0, new Vec3(1,Y-1,0), new Vec3(0,1,0));
  await place('连接器', 2,Y,0, new Vec3(2,Y-1,0), new Vec3(0,1,0));
  await place('太阳能', 3,Y,0, new Vec3(3,Y-1,0), new Vec3(0,1,0));
  await place('淘金盘', 2,Y,1, new Vec3(2,Y-1,1), new Vec3(0,1,0));
  log('solar layout verify:');
  for (const p of [[0,Y,0],[1,Y,0],[2,Y,0],[3,Y,0],[2,Y,1]]) log(' ', p.join(','), bot.blockAt(new Vec3(...p)).name);

  // menu smoke: right-click the gold pan machine
  bot.activateBlockBlocks = null;
  await rc(`/tp AuditBot 4.5 102.5 2.5`); await sleep(300);
  await bot.lookAt(new Vec3(2.5, Y+0.5, 1.5)); await sleep(200);
  bot.activateBlock(bot.blockAt(new Vec3(2,Y,1)));
  await sleep(2500);
  let win = bot.currentWindow;
  if (win) { log('MACHINE MENU OPEN slots=', win.slots.length);
    const cn = win.slots.filter(Boolean).map(customName).join('');
    log('menu Chinese customNames present:', /[一-鿿]/.test(cn));
    const named = win.slots.filter(Boolean).map(s => s.name + ':' + (customName(s).match(/[一-鿿]{2,8}/) || [''])[0]).filter(x => !x.startsWith('gray_stained')).slice(0, 10);
    log('menu items:', named.join(' | '));
    bot.closeWindow(win);
  } else log('NO MENU WINDOW');

  // ===== cargo: M(0,101,4) inNode(1,101,4) chestA(2,101,4) outNode(-1,101,4) chestB(-2,101,4)
  const chest = bot.inventory.items().find(i => i.name === 'chest' && !customName(i));
  await bot.equip(chest, 'hand'); await sleep(150);
  await rc(`/tp AuditBot 3.5 102.5 5.5`); await sleep(300);
  await bot.lookAt(new Vec3(2.5, Y+0.5, 4.5)); await sleep(150);
  try { await bot.placeBlock(bot.blockAt(new Vec3(2,Y-1,4)), new Vec3(0,1,0)); } catch {}
  await sleep(600); log('chestA:', bot.blockAt(new Vec3(2,Y,4)).name);
  await rc(`/tp AuditBot -1.5 102.5 5.5`); await sleep(300);
  await bot.lookAt(new Vec3(-2.5, Y+0.5, 4.5)); await sleep(150);
  try { await bot.placeBlock(bot.blockAt(new Vec3(-2,Y-1,4)), new Vec3(0,1,0)); } catch {}
  await sleep(600); log('chestB:', bot.blockAt(new Vec3(-2,Y,4)).name);
  await place('管理器', 0,Y,4, new Vec3(0,Y-1,4), new Vec3(0,1,0));
  await place('输入', 1,Y,4, new Vec3(1,Y-1,4), new Vec3(0,1,0));
  await place('输出', -1,Y,4, new Vec3(-1,Y-1,4), new Vec3(0,1,0));
  log('cargo layout:', [0,1,2,-1,-2].map(x => x+','+bot.blockAt(new Vec3(x,Y,4)).name).join(' ; '));

  // load coal into chestA
  const coal = bot.inventory.items().find(i => i.name === 'coal');
  if (coal) {
    const cont = await bot.openContainer(bot.blockAt(new Vec3(2,Y,4)));
    await cont.deposit(coal.type, null, 16).catch(e => log('deposit err', e.message));
    log('chestA container:', cont.containerItems().map(i=>i.name+'x'+i.count).join(',') || '(empty)'); cont.close();
  }
  log('waiting 45s for cargo + autosave...');
  await sleep(45000);
  const cb = bot.blockAt(new Vec3(-2,Y,4));
  if (cb && cb.name === 'chest') { const cont = await bot.openContainer(cb);
    log('CHEST B CONTENTS:', cont.containerItems().map(i => i.name + 'x' + i.count).join(', ') || '(empty)'); cont.close();
  } else log('chestB missing');
  bot.quit(); await sleep(800); rcon.end(); process.exit(0);
}
main().catch(e => { console.error(e); process.exit(1); });
