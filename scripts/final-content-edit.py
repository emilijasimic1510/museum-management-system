from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
def edit(path,fn):
 p=root/path;s=p.read_text(encoding='utf-8');p.write_text(fn(s),encoding='utf-8')
# Preserve the original image generator only for byte-exact recognition of old covers.
p='backend/src/main/java/rs/ac/uns/ftn/museum/config/LocalImageSeeder.java'
def images(s):
 s=s.replace('Original geometric demo illustrations, generated locally.', 'Open-access thematic reference images from The Metropolitan Museum of Art.')
 s=s.replace('private void putMissing(String key,int variant){if(!storage.exists(key))storage.write(key,illustration(variant));}', '''private void putMissing(String key,int variant){
        if(!storage.exists(key) || java.util.Arrays.equals(storage.read(key),legacyIllustration(variant)))
            storage.write(key,rs.ac.uns.ftn.museum.media.CollectionImages.image(variant));
    }
    public static byte[] illustration(int variant){return rs.ac.uns.ftn.museum.media.CollectionImages.image(variant);}
    private static byte[] legacyIllustration(int variant){'''.rstrip('{'))
 s=s.replace('public static byte[] illustration(int variant){\n        var image', 'private static byte[] legacyIllustration(int variant){\n        var image')
 # remove accidental duplicate declaration from replacement
 s=s.replace('    private static byte[] legacyIllustration(int variant)\n    private static byte[] legacyIllustration', '    private static byte[] legacyIllustration')
 return s
edit(p,images)
for p in (root/'frontend/public/demo').rglob('*.txt'):
 s=p.read_text(encoding='utf-8').replace('Lokalni demonstracioni sadržaj.','Beleška iz muzejskog vodiča.').replace('Audio snimak nije dostupan; ovaj tekst služi kao lokalna demonstracija sadržaja.','Posmatrajte odnos svetla, prostora i tragova svakodnevnog života.')
 p.write_text(s,encoding='utf-8')
