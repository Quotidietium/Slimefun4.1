default: help

.PHONY: help
help:
	@echo "  * gen-biomes version=<version> - 生成指定版本的生物群系（biomes）数据"

.PHONY: gen-biomes
gen-biomes:
	@echo "正在为 $(version) 生成生物群系数据"
	@curl "https://raw.githubusercontent.com/MockBukkit/MockBukkit/refs/heads/v$(version)/src/main/resources/keyed/worldgen/biome.json" -s \
		| jq '[ .values[].key]' \
		> "src/test/resources/biomes/$(version).x.json"
