from pathlib import Path
root=Path(__file__).parent
accounts={
'visitor':('ana.petrovic','Ana','Petrović'), 'staff':('milica.jovanovic','Milica','Jovanović'),
'curator':('nikola.markovic','Nikola','Marković'),'guide':('jelena.stojanovic','Jelena','Stojanović'),
'manager':('ivan.nikolic','Ivan','Nikolić'),'admin':('marija.ilic','Marija','Ilić')}
replacements={f'demo.{k}@museum.local':f'{v[0]}@museum.local' for k,v in accounts.items()}
replacements.update({'Demo Museum Novi Sad':'Muzej gradskih priča','Demo Muzej savremenih tokova':'Muzej savremenih tokova','Demo Galerija obalske fotografije':'Galerija obalske fotografije','Demo Muzej forme i zanata':'Muzej forme i zanata','Demo razgovor o svetlosti':'Razgovor o svetlosti','Demo radionica boje':'Radionica boje i pigmenta','Demo otkazana tura':'Šetnja kroz skulpturu','Demo vođena tura':'Tragovi svetlosti — vođeni obilazak','Demo cenovnik':'Redovni cenovnik','Demo autor lokalne zbirke.':'Autor posvećen odnosu svetlosti, materijala i svakodnevnog života.','Bezbedna razvojna zbirka za demonstraciju muzejske aplikacije.':'Zbirka umetnosti i vizuelne kulture posvećena gradskom životu, svetlosti i sećanju.'})
for p in (root/'backend/src').rglob('*.java'):
 s=p.read_text(encoding='utf-8')
 for a,b in replacements.items():s=s.replace(a,b)
 if p.name=='DemoDataSeeder.java':
  for role,label in [('visitor','Posetilac'),('staff','Zaposleni'),('curator','Kustos'),('guide','Vodič'),('manager','Menadžer'),('admin','Administrator')]:
   s=s.replace(f'"Demo", "{label}"',f'"{accounts[role][1]}", "{accounts[role][2]}"')
  s=s.replace('public void seed() {','public void seed() {\n        LocalContentUpgrade.apply(jdbc);')
 if p.name=='RichDemoDataSeeder.java':
  s=s.replace('demo.history.01@museum.local','ana.ristic@museum.local')
  s=s.replace('"demo.history.%02d@museum.local".formatted(i + 1)','LocalContentUpgrade.historyEmail(i)')
  s=s.replace(' + " Izložba pripada izmišljenoj lokalnoj muzejskoj zbirci."','')
  s=s.replace(' + " Izmišljena autorska biografija za lokalnu zbirku."','')
  s=s.replace('Izmišljeni muzej','Muzej').replace('Izmišljena galerija','Galerija')
  s=s.replace('zamišljeni atelje','atelje').replace('izmišljena radionička','radionička').replace('zamišljeni autorski','autorski').replace('izmišljeni studijski','studijski')
  s=s.replace('Autorski rad u izmišljenoj zbirci.','Autorski rad u muzejskoj zbirci.')
 if p.name=='RichDemoDataSeederTests.java':s=s.replace('demo.history.01@museum.local','ana.ristic@museum.local')
 if s!=p.read_text(encoding='utf-8'):p.write_text(s,encoding='utf-8')
p=root/'frontend/src/app/core/i18n.ts';s=p.read_text(encoding='utf-8')
for a,b in {'SAMO LOKALNA DEMONSTRACIJA':'MUZEJSKI VODIČ','LOCAL DEMONSTRATION ONLY':'MUSEUM GUIDE','Demo QR tabla':'QR tabla postavki','Demo QR board':'Installation QR board','Demo skeniranje · samo lokalno':'Izaberite postavku','Demo scanning · local only':'Choose an installation','Iznos iz važećeg cenovnika. Online kupovina koristi karticu. Lokalna demonstracija ne vrši stvarnu naplatu.':'Iznos iz važećeg cenovnika. Izaberite kategoriju ulaznice i potvrdite kupovinu.','Amount from the current price list. Online purchases use a card. The local demo does not charge real money.':'Amount from the current price list. Choose a ticket category and confirm your purchase.','Backend nije dostupan. Proverite da li je pokrenut na portu 8081.':'Usluga trenutno nije dostupna. Pokušajte ponovo za nekoliko trenutaka.'}.items():s=s.replace(a,b)
p.write_text(s,encoding='utf-8')
p=root/'frontend/src/app/pages/curator-content.ts';s=p.read_text(encoding='utf-8').replace('placeholder="demo/grad-001.txt"','placeholder="collection/grad-001.txt"');p.write_text(s,encoding='utf-8')
