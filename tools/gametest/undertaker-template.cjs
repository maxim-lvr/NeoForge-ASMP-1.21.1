const fs = require('node:fs');
const zlib = require('node:zlib');
const int = n => {const b=Buffer.alloc(4); b.writeInt32BE(n); return b;};
const str = s => {const b=Buffer.from(s);const l=Buffer.alloc(2);l.writeUInt16BE(b.length);return Buffer.concat([l,b]);};
const tag = (type,name,data) => Buffer.concat([Buffer.from([type]),str(name),data]);
const list = (type,entries) => Buffer.concat([Buffer.from([type]),int(entries.length),...entries]);
const nbt = tag(10,'',Buffer.concat([
    tag(3,'DataVersion',int(3955)),
    tag(9,'size',list(3,[int(40),int(8),int(12)])),
    tag(9,'palette',list(10,[Buffer.concat([tag(8,'Name',str('minecraft:air')),Buffer.from([0])])])),
    tag(9,'blocks',list(10,[])),tag(9,'entities',list(10,[])),Buffer.from([0])
]));
const dir='/tmp/undertaker-resources/data/asmpthingsmod/structure';
fs.mkdirSync(dir,{recursive:true});
fs.writeFileSync(dir+'/undertaker_empty.nbt',zlib.gzipSync(nbt));
