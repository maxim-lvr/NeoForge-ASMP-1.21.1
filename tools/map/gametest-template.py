"""Create an empty test structure; run before the map GameTests."""
import gzip
import struct
import json
from pathlib import Path


def integer(value):
    return struct.pack('>i', value)


def string(value):
    encoded = value.encode()
    return struct.pack('>H', len(encoded)) + encoded


def tag(kind, name, data):
    return bytes([kind]) + string(name) + data


def sequence(kind, entries):
    return bytes([kind]) + integer(len(entries)) + b''.join(entries)


nbt = tag(10, '', b''.join([
    tag(3, 'DataVersion', integer(3955)),
    tag(9, 'size', sequence(3, [integer(64), integer(64), integer(64)])),
    tag(9, 'palette', sequence(10, [tag(8, 'Name', string('minecraft:air')) + b'\0'])),
    tag(9, 'blocks', sequence(10, [])),
    tag(9, 'entities', sequence(10, [])),
    b'\0',
]))
directory = Path('/tmp/asmp-map-resources/data/asmpthingsmod/structure')
directory.mkdir(parents=True, exist_ok=True)
(directory / 'map_empty.nbt').write_bytes(gzip.compress(nbt))

# GameTestServer constructs its dimensions from the flat preset, ignoring standalone dimension files.
# Extend that preset only in these temporary test resources so the real command can resolve ASMP.
preset = {
    'dimensions': {
        'minecraft:overworld': {
            'type': 'minecraft:overworld',
            'generator': {'type': 'minecraft:flat', 'settings': {
                'biome': 'minecraft:plains', 'features': False, 'lakes': False, 'structure_overrides': [],
                'layers': [{'block': 'minecraft:bedrock', 'height': 1},
                           {'block': 'minecraft:dirt', 'height': 2},
                           {'block': 'minecraft:grass_block', 'height': 1}],
            }},
        },
        'minecraft:the_nether': {
            'type': 'minecraft:the_nether',
            'generator': {'type': 'minecraft:noise', 'settings': 'minecraft:nether',
                          'biome_source': {'type': 'minecraft:multi_noise', 'preset': 'minecraft:nether'}},
        },
        'minecraft:the_end': {
            'type': 'minecraft:the_end',
            'generator': {'type': 'minecraft:noise', 'settings': 'minecraft:end',
                          'biome_source': {'type': 'minecraft:the_end'}},
        },
        'asmpthingsmod:asmp_dimension': json.loads(
            Path('src/main/resources/data/asmpthingsmod/dimension/asmp_dimension.json').read_text()),
    },
}
flat = Path('/tmp/asmp-map-resources/data/minecraft/worldgen/world_preset/flat.json')
flat.parent.mkdir(parents=True, exist_ok=True)
flat.write_text(json.dumps(preset))
