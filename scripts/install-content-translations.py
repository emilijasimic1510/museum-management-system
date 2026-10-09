from pathlib import Path
import re,json
root=Path(__file__).resolve().parents[1]
catalog=(root/'backend/src/main/java/rs/ac/uns/ftn/museum/config/RichDemoCatalog.java').read_text(encoding='utf-8')
def rows(key):return [x.strip().split('|') for x in re.search(key+r' = rows\("""(.*?)"""\)',catalog,re.S)[1].strip().splitlines()]
works,shows=rows('WORKS'),rows('SHOWS')
ew=[x.split('|') for x in (root/'scripts/collection-en.txt').read_text(encoding='utf-8').splitlines()]
es=[x.split('|') for x in (root/'scripts/shows-en.txt').read_text(encoding='utf-8').splitlines()]
assert len(works)==len(ew)==56 and len(shows)==len(es)==12
pairs={}
for w,e in zip(works,ew):pairs[w[0]]=e[0];pairs[w[3]]=e[1]
for s,e in zip(shows,es):
 pairs[s[0]]=e[0];pairs[s[1]]=e[1];pairs[s[6]]=e[2];pairs[s[7]]=e[3]
 pairs[s[1]+'. '+s[6]+' '+s[7]]=e[1]+'. '+e[2]+' '+e[3]
 for sr,en in [('Porodična radionica','Family workshop'),('Vođeno čitanje','Guided interpretation'),('Razgovor sa kustosom','Conversation with the curator'),('Radionica posmatranja','Observation workshop')]:pairs[sr+' · '+s[0]]=en+' · '+e[0]
 for slot in range(8):
  group=int(s[3]);ix=group*7+(slot+shows.index(s))%7 if slot<7 else ((group+1)%8)*7+shows.index(s)%7
  w,translated=works[ix],ew[ix]
  original='Na izložbi „'+s[0]+'“, delo „'+w[0]+'“ uvodi temu „'+s[1].lower()+'“. '+s[6]+' '+w[3].split('. ')[0]+'. '+s[7]
  pairs[original]='In “'+e[0]+'”, “'+translated[0]+'” introduces the theme of '+e[1].lower()+'. '+e[2]+' '+translated[1].split('. ')[0]+'. '+e[3]
  pairs[s[0]+' · celina '+str(slot//2+1)+' · mesto '+str(slot+1)]=e[0]+' · section '+str(slot//2+1)+' · position '+str(slot+1)
pairs.update(dict(line.split('|',1) for line in '''Tragovi svetlosti|Traces of Light
Tri eksponata, jedan autor i put kroz boju, materijal i sećanje.|Three exhibits, one artist and a journey through colour, material and memory.
Veče skulpture|An Evening of Sculpture
Skulpture i materijali u dijalogu.|Sculptures and materials in dialogue.
Grad kroz objektiv|The City through a Lens
Tri pogleda na grad: jutro, reka i noć.|Three views of the city: morning, river and night.
Tišina u plavom|Silence in Blue
Dnevnik svetlosti.|A diary of light.
Dodir prostora|Touch of Space
Materijal kao sećanje.|Material as memory.
Pre nego što svane|Before Dawn
Grad između dva trenutka.|The city between two moments.
Krug u bronzi|Circle in Bronze
Odnos pune forme i praznog prostora.|The relationship between solid form and empty space.
Ritam kamena|Rhythm of Stone
Ponavljanje oblika i promena perspektive.|Repeating forms and changing perspectives.
Linija u pokretu|Line in Motion
Svetlost i senka u pokretu posetioca.|Light and shadow as the visitor moves.
Jutro na trgu|Morning in the Square
Odraz na Dunavu|Reflection on the Danube
Noćna svetla|Night Lights
Počnite obilazak svetlom jutra na gradskom trgu.|Begin with morning light in the city square.
Uporedite arhitekturu grada sa njenim odrazom na reci.|Compare the city's architecture with its reflection in the river.
Završite obilazak kontrastima svetla i senke posle zalaska sunca.|Finish with contrasts of light and shadow after sunset.
Razgovor o svetlosti|A Conversation about Light
Radionica boje i pigmenta|Colour and Pigment Workshop
Šetnja kroz skulpturu|A Walk through Sculpture
Tragovi svetlosti — vođeni obilazak|Traces of Light — Guided Tour
Muzej gradskih priča|Muzej gradskih priča
Muzej savremenih tokova|Muzej savremenih tokova
Galerija obalske fotografije|Galerija obalske fotografije
Muzej forme i zanata|Muzej forme i zanata
Zbirka umetnosti i vizuelne kulture posvećena gradskom životu, svetlosti i sećanju.|A collection of art and visual culture dedicated to urban life, light and memory.
Muzej posvećen savremenoj umetnosti i susretu različitih materijala. Program povezuje skulpturu, slikarstvo i male prostorne eksperimente.|A museum of contemporary art and encounters between materials. Its programme brings together sculpture, painting and small spatial experiments.
Galerija za fotografiju i grafiku sa čitaonicom vizuelnih dnevnika. Njena zbirka prati obalu, gradske rubove i svakodnevne putanje.|A photography and print gallery with a reading room for visual diaries. Its collection explores the shore, urban edges and everyday routes.
Muzej skulpture i primenjene umetnosti. Zbirka približava posetiocima odnos ručnog rada, industrijskog oblikovanja i života predmeta.|A museum of sculpture and applied art. The collection explores the relationship between craft, industrial design and the lives of objects.
Autor|Artist
Autor koncepta i realizacije|Concept and execution
Dizajn i prototip|Design and prototype
Konstrukcija i prostorna realizacija|Construction and spatial execution
Tekstilni i svetlosni elementi|Textile and lighting elements
Autor posvećen odnosu svetlosti, materijala i svakodnevnog života.|An artist exploring the relationship between light, materials and everyday life.
Redovni cenovnik|Standard price list
Lokalna zbirka · cena izražena u RSD|Collection admission · price in RSD
Ulje na platnu|Oil on canvas
Drvo i metal|Wood and metal
Analogna fotografija|Analogue photography
Digitalna fotografija|Digital photography
Bronza|Bronze
Kamen|Stone
Metal|Metal
Srbija|Serbia
Vojvodina|Vojvodina
Novi Sad|Novi Sad
SLIKA|Painting
SKULPTURA|Sculpture
FOTOGRAFIJA|Photography
CRTEZ|Drawing
GRAFIKA|Print
INSTALACIJA|Installation
PRIMENJENA_UMETNOST|Applied art
ODRASLI|Adult
STUDENT|Student
PENZIONER|Senior
DETE|Child
Galerija svetlosti|Gallery of Light
Galerija susreta|Gallery of Encounters
Galerija gradskih priča|Gallery of City Stories
Velika galerija|Great Gallery
Skulptorski salon|Sculpture Salon
Sala moderne umetnosti|Modern Art Gallery
Grafički kabinet|Print Room
Galerija predmeta|Object Gallery
Mala galerija|Small Gallery
Portretni salon|Portrait Salon
Atrijum|Atrium
Galerija fotografije|Photography Gallery'''.splitlines()))
materials='''Archival pigment print|Silver gelatin print|Pigment print on cotton paper|Colour photographic print|Analogue colour print|Digital pigment print|Black-and-white print on baryta paper|Patinated bronze|Fired clay|Welded steel|Hand-worked limestone|Cast aluminium|Ash wood and brass|Stone and painted steel|Oil on linen canvas|Acrylic on canvas|Graphite and chalk on paper|Tempera on wood panel|Oil and wax on canvas|Pastel on toned paper|Acrylic and graphite on canvas|Linocut on Japanese paper|Drypoint|Three-colour screenprint|Woodcut|Ink in a hand-bound book|Aquatint|Monotype|Steel, brass and opal glass|Beech and woven tape|Plywood and aluminium|Glazed ceramic|Walnut and felt|Aluminium and coloured glass|Wool and linen|Oil on canvas|Series of pigment prints|Graphite on cotton paper|Acrylic on canvas|Macro photographic print|Ink and watercolour|Tempera on canvas|Oil on canvas|Charcoal on paper|Acrylic on canvas|Colour pigment print|Pastel and graphite|Oil on wood panel|Sepia on paper|Cotton thread and wooden frames|Textile and plaster casts|Wood, steel and springs|Reused wood|Folded paper and nylon|Wood, ceramic and linen|Steel wire and textile strips'''.split('|')
for w,m in zip(works,materials):pairs[w[2]]=m
for sr,en in [('Novi Sad — atelje na obali','Novi Sad — riverside studio'),('Beograd — radionička zbirka','Belgrade — workshop collection'),('Zrenjanin — autorski legat','Zrenjanin — artist bequest'),('Subotica — studijski ciklus','Subotica — studio series')]:pairs[sr]=en
p=root/'frontend/src/app/core/i18n.ts';s=p.read_text(encoding='utf-8')
block='// Curated collection translations. Exact source matching preserves curator edits and SR fallback.\nconst COLLECTION_EN:Record<string,string>='+json.dumps(pairs,ensure_ascii=False,indent=2)+';\nfor(const [index,[sr,en]] of Object.entries(COLLECTION_EN).entries()){SR["collection."+index]=sr;EN["collection."+index]=en;}\nSR["media.reference"]="Tematska ilustracija, nije fotografija eksponata:";EN["media.reference"]="Thematic reference, not a photograph of this exhibit:";\n'
s=s.replace('// Shared with non-injectable presentation helpers',block+'// Shared with non-injectable presentation helpers')
s+='''
/** Presentation only: never writes translated values back to an editor or API. */
export function localizedContent(record:unknown,field:string):string {
  if(!record||typeof record!=='object')return '';
  const row=record as Record<string,unknown>,value=row[field];
  const sr=typeof value==='string'?value:typeof value==='object'&&value?(value as Record<string,string>)['sr']||'':'';
  if(selectedLanguage()==='sr')return sr;
  const english=row[field+'En']??(row['translations'] as Record<string,Record<string,string>>|undefined)?.['en']?.[field]??(typeof value==='object'&&value?(value as Record<string,string>)['en']:undefined);
  return typeof english==='string'&&english.trim()?english:translate(sr);
}
@Pipe({name:'localized',pure:false}) export class LocalizedContentPipe implements PipeTransform {
  constructor(private i18n:I18n){}
  transform(record:unknown,field:string){this.i18n.language();return localizedContent(record,field);}
}
'''
p.write_text(s,encoding='utf-8')
# Localize display bindings without modifying form models or network payloads.
for p in (root/'frontend/src/app').rglob('*.ts'):
 if p.name.endswith('.spec.ts') or 'template:' not in p.read_text(encoding='utf-8'):continue
 s=p.read_text(encoding='utf-8');original=s
 fields='name|description|contextDescription|exhibitionName|exhibitName|material|origin|biography|contributionType|position|category'
 pattern=r'{{\s*([\w]+(?:\.[\w]+)*)\.('+fields+r')\s*(\|\|\s*\([^{}]+\))?\s*}}'
 s=re.sub(pattern,lambda m:'{{('+m[1]+"|localized:'"+m[2]+"')"+(m[3] or '')+'}}',s)
 s=re.sub(r'\[alt\]="([\w.]+)\.name"',r'''[alt]="\1|localized:'name'"''',s)
 if s!=original:
  s="import { LocalizedContentPipe } from '../core/i18n';\n"+s
  s=s.replace('imports:[','imports:[LocalizedContentPipe,')
 s=s.replace('>ADMINISTRATOR<',">{{'role.ADMINISTRATOR'|t}}<")
 p.write_text(s,encoding='utf-8')
