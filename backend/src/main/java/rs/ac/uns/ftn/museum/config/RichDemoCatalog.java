package rs.ac.uns.ftn.museum.config;

import java.util.List;

/** Authored, fictional collection. Row order is part of the stable inventory numbering. */
final class RichDemoCatalog {
    private RichDemoCatalog() {}
    static List<String[]> rows(String text) {
        return text.strip().lines().map(line -> line.split("\\|", -1)).toList();
    }
    // First name | last name | birth year | practice and biography
    static final List<String[]> AUTHORS = rows("""
        Dunja|Vidaković|1978|Fotografkinja koja prati promene rečnih četvrti. U svom radu povezuje sporo posmatranje i arhiviranje svakodnevnih prizora.
        Ognjen|Radanov|1983|Fotograf posvećen noćnom gradskom pejzažu. Snima iz pešačke perspektive, bez uklanjanja tragova svakodnevnog života.
        Iva|Sretenović|1990|Autorka dokumentarnih fotografskih ciklusa o javnom prevozu. Beleži male gestove putnika i promene svetla u pokretu.
        Vuk|Milenkov|1971|Vajar koji istražuje ravnotežu i težinu. Ručno obrađene površine njegovih radova čuvaju trag alata i postupka nastanka.
        Neda|Radulović|1986|Vajarka usmerena na glinu i livenu bronzu. Oblike razvija iz studija obalnog kamenja i rečnih nanosa.
        Pavle|Tešin|1968|Autor prostornih skulptura od metala. Spaja tehnički crtež sa iskustvom rada u malim zanatskim radionicama.
        Lenka|Dobrić|1982|Slikarka koja ispituje odnose toplih i hladnih površina. Radi u tankim slojevima pigmenta kroz koje ostaju vidljive ranije odluke.
        Relja|Obradin|1975|Slikar urbanih enterijera i odraza. Njegova paleta potiče od boja zidova, prozora i starih podnih pločica.
        Tara|Veselin|1992|Autorka svetlosnih studija na papiru. Prati promenu istog motiva tokom dana i beleži razlike u opažanju.
        Nađa|Kosanović|1980|Grafičarka koja radi linorez i suvu iglu. U ciklusima o susedstvu istražuje ritam ponavljanja i odstupanja.
        Filip|Zorić|1988|Grafičar i ilustrator posvećen tipografiji. Ručno slaže slova i pronalazi likovni ritam u svakodnevnim natpisima.
        Una|Berić|1994|Autorka crteža i malih grafičkih knjiga. Beleži kretanje kroz grad nizovima kratkih linija i praznih polja.
        Marko|Velimirov|1965|Dizajner upotrebnih predmeta. Razvija prototipe koji se mogu popravljati jednostavnim alatima i zamenom pojedinih delova.
        Jelisaveta|Grujičin|1984|Dizajnerka nameštaja i tekstila. Polazi od navika korisnika i istražuje kako se predmeti prilagođavaju malim prostorima.
        Danilo|Šarčev|1979|Dizajner svetiljki i radioničke opreme. Posebnu pažnju posvećuje spojevima, dostupnosti materijala i dugom veku predmeta.
        Maša|Rakićev|1987|Slikarka rečnih pejzaža. Terenske beleške pretvara u platna koja prate sezonske promene obale.
        Bojan|Lukićev|1973|Fotograf biljnih zajednica u gradu. Snima rubne prostore u kojima se ukrštaju planirano zelenilo i spontani rast.
        Vera|Ninkov|1962|Autorka crteža prirodnih struktura. Višegodišnje posmatranje stabala i semena povezuje sa preciznim grafitnim studijama.
        Sofija|Todorin|1991|Portretistkinja koja radi sa dobrovoljnim učesnicima. Njeni portreti nastaju kroz razgovor i ponovljene susrete u ateljeu.
        Andrej|Stojanov|1981|Slikar malih intimnih formata. Umesto reprezentativnih poza bira trenutke odmora i odsutnog pogleda.
        Milena|Dražićev|1970|Autorka portretnih crteža ugljenom. Istražuje kako položaj ruku i ramena može preneti raspoloženje bez mnogo detalja.
        Iskra|Pejović|1993|Autorka prostornih instalacija od tekstila. Poziva publiku da uspori hod i obrati pažnju na granice zajedničkog prostora.
        Uroš|Banjac|1985|Autor zvučnih i mehaničkih instalacija. Koristi male pokrete i tihe ritmove umesto spektakularnih efekata.
        Elena|Mirković|1977|Autorka instalacija od pronađenih materijala. Predmete grupiše prema tragovima upotrebe, a ne prema njihovoj tržišnoj vrednosti.
        Teodor|Savićev|1989|Saradnik na konstrukcijama i postavkama kolektivnih radova. Razvija rastavljive nosače koji ostavljaju vidljiv način sklapanja.
        Ksenija|Perin|1983|Autorka tekstilnih i svetlosnih elemenata zajedničkih instalacija. Istražuje kako senka menja doživljaj mekane površine.
        """);
    // Title | type | material | specific observation. Seven works per thematic group.
    static final List<String[]> WORKS = rows("""
        Poslednji tramvaj|FOTOGRAFIJA|Arhivski pigmentni otisak|Prazna stanica i udaljena svetla vozila dele kadar na prostor čekanja i prostor odlaska. Vlažan kolovoz zadržava odraz nakon što se izvor svetla izgubi.
        Prozor iznad pijace|FOTOGRAFIJA|Srebrno-želatinski otisak|Pogled sa sprata otkriva geometriju tezgi dok prolaznici remete njen pravilan raster. Ivice prozora ostaju u kadru kao znak prisustva posmatrača.
        Obala u magli|FOTOGRAFIJA|Pigmentni otisak na pamučnom papiru|Tanka linija pristaništa razdvaja vodu od magle. Sitna figura na kraju mola daje meru prizoru koji gotovo ostaje bez horizonta.
        Stepenice posle kiše|FOTOGRAFIJA|Kolor fotografski otisak|Na istrošenim stepenicama smenjuju se suve i mokre površine. Odraz neba privremeno prekriva tragove dugog korišćenja.
        Čekaonica u podne|FOTOGRAFIJA|Analogni kolor otisak|Red praznih stolica presečen je senkom prozorske prečke. Jedna ostavljena torba pretvara miran enterijer u priču o kratkom odsustvu.
        Krovovi sa severa|FOTOGRAFIJA|Digitalni pigmentni otisak|Slojevi limenih i crepnih krovova pokazuju različite starosti iste četvrti. Antene i improvizovane popravke prekidaju ponavljanje kosih ravni.
        Bicikl uz kapiju|FOTOGRAFIJA|Crno-beli otisak na baritnom papiru|Točak bicikla ponavlja luk metalne kapije. Trag gume u prašini uvodi kretanje u inače nepomičan prizor.
        Težište|SKULPTURA|Patinirana bronza|Dve zaobljene mase dodiruju se na uskoj površini. Vidljiva težina gornjeg dela suprotstavlja se mirnom položaju celine.
        Nabor obale|SKULPTURA|Pečena glina|Slojevi gline savijeni su kao povučena rečna obala. Neravne ivice čuvaju trag pritiska prstiju i različitog sušenja.
        Most bez reke|SKULPTURA|Zavareni čelik|Tanka lučna konstrukcija oslanja se na dva nejednaka stuba. Praznina ispod luka postaje jednako važna kao metal koji je omeđuje.
        Seme od kamena|SKULPTURA|Ručно obrađen krečnjak|Zatvorena forma ima plitak usek koji podseća na mesto klijanja. Različito polirane površine menjaju ton pri obilasku.
        Otvoreni prsten|SKULPTURA|Liveni aluminijum|Krug je prekinut taman toliko da pogled može da prođe kroz rascep. Unutrašnja hrapava površina suprotstavlja se glatkoj spoljašnjoj strani.
        Složeni vetar|SKULPTURA|Drvo jasena i mesing|Niz tankih elemenata postavljen je pod različitim uglovima. Njihove senke sugerišu pokret iako je konstrukcija nepomična.
        Tri oslonca|SKULPTURA|Kamen i obojeni čelik|Tri nosača različitih visina nose jednu ravnu ploču. Neravnomerna raspodela oslonaca traži pažljivo posmatranje ravnoteže.
        Popodnevni zid|SLIKA|Ulje na lanenom platnu|Toplo obojeno polje presečeno je hladnom senkom prozora. Sitne nepravilnosti poteza čuvaju osećaj zida koji upija dnevnu svetlost.
        Plava soba u šest|SLIKA|Akrilik na platnu|Enterijer je građen skoro isključivo nijansama plave. Uska narandžasta ivica vrata menja doživljaj dubine i temperature prostora.
        Senka smokve|CRTEZ|Grafit i kreda na papiru|Preklopljene senke listova stvaraju gustu mrežu mekih ivica. Prazna površina papira označava mesta najjače svetlosti.
        Zavesa prema zapadu|SLIKA|Tempera na drvenoj ploči|Tkanina propušta svetlost u nejednakim prugama. Naslikani nabori istovremeno opisuju predmet i dele površinu slike na ritmična polja.
        Beli sto|SLIKA|Ulje i vosak na platnu|Površina stola izgrađena je belim tonovima različite topline. Senka male činije otkriva dubinu koju sama bela boja gotovo prikriva.
        Linija sumraka|CRTEZ|Pastel na toniranom papiru|Uska svetla traka ostaje između tamne fasade i neba. Razmazani pigment beleži trenutak kada se obrisi počinju gubiti.
        Refleks u hodniku|SLIKA|Akrilik i grafit na platnu|Odraz prozora prelazi sa poda na zid. Perspektivne linije vode prema izvoru svetla koji se ne nalazi u samom kadru.
        Mapa susedstva|GRAFIKA|Linorez na japanskom papiru|Ulice su prikazane kao nizovi belih prolaza između crnih blokova. Jedna nepravilna putanja čuva sećanje na pešačku prečicu.
        Sedam prolaza|GRAFIKA|Suva igla|Sedam otvora ponavlja isti osnovni oblik uz sitne razlike u pritisku linije. Najtamniji prolaz nema najviše detalja, već najgušći splet tragova.
        Slova sa fasade|GRAFIKA|Sitoštampa u tri boje|Fragmenti izmišljenih radnji složeni su u novu tipografsku celinu. Preklapanje boja pokazuje redosled štampe i ostavlja vidljiv postupak rada.
        Ritam balkona|GRAFIKA|Drvorez|Pravougaona polja balkona raspoređena su u nepravilan niz. Duboki rezovi menjaju težinu crnih površina i osećaj blizine fasade.
        Knjiga kratkih šetnji|CRTEZ|Tuš u ručno povezanoj knjizi|Svaki list beleži jednu kratku putanju kroz naselje. Prazne stranice označavaju zastajanje, a ne nedovršen rad.
        Kišni raster|GRAFIKA|Akvatinta|Sitne tačke postepeno prelaze iz svetlog u tamno polje. Gustina rastera priziva pogled kroz staklo prekriveno kišom.
        Trag točka|GRAFIKA|Monotipija|Kružni trag prekida ravnomeran nanos štamparske boje. Otisak čuva jedan neponovljiv susret pritiska, papira i pigmenta.
        Lampa za čitanje|PRIMENJENA_UMETNOST|Čelik, mesing i opalno staklo|Zglobni nosač usmerava svetlo bez pomeranja teške baze. Vidljivi vijci pokazuju da se pojedini delovi mogu zameniti i popraviti.
        Stolica na sklapanje|PRIMENJENA_UMETNOST|Bukva i tkana traka|Ravna konstrukcija postaje sedište jednim pokretom. Zategnuta traka prilagođava se telu i smanjuje potrebu za dodatnim materijalom.
        Radio za radionicu|PRIMENJENA_UMETNOST|Šperploča i aluminijum|Velika dugmad predviđena su za ruke zauzete radom. Kućište ima zamenljivu zaštitnu mrežu i ručku koja ujedno štiti kontrole.
        Čajnik za dvoje|PRIMENJENA_UMETNOST|Glazirana keramika|Kratak izliv i široka drška prilagođeni su sporom sipanju. Matirana glazura ostavlja vidljive promene debljine zida posude.
        Kutija sa pregradama|PRIMENJENA_UMETNOST|Orah i filc|Pokretne pregrade dopuštaju različite rasporede sitnih predmeta. Spojevi su vidljivi i svedoče o načinu ručne izrade.
        Sat bez brojeva|PRIMENJENA_UMETNOST|Aluminijum i obojeno staklo|Dva obojena kruga zamenjuju uobičajene kazaljke. Njihovo preklapanje pretvara čitanje vremena u posmatranje promene boje.
        Tkanje za mali stan|PRIMENJENA_UMETNOST|Vuna i lan|Gusti i retki delovi tkanja menjaju propustljivost svetlosti. Predmet služi kao lagana pregrada koja ne zatvara prostor potpuno.
        Trska u januaru|SLIKA|Ulje na platnu|Suve stabljike izdvajaju se iz hladne površine vode. Nekoliko savijenih linija pokazuje smer vetra bez prikazivanja samog pokreta.
        Herbarium trotoara|FOTOGRAFIJA|Serija pigmentnih otisaka|Biljke rastu u spojevima pločnika i uz rubove zidova. Niski ugao snimanja daje tim malim staništima razmere pejzaža.
        Godovi|CRTEZ|Grafit na pamučnom papiru|Koncentrične linije nisu pravilni krugovi već zapisi nejednakog rasta. Pukotina prekida ritam i otkriva unutrašnji napon drveta.
        Ostrvo posle poplave|SLIKA|Akrilik na platnu|Naslage mulja i granja formiraju privremenu obalu. Mirna paleta ostavlja prostor da se uoče sitni tragovi povlačenja vode.
        Mahovina na cigli|FOTOGRAFIJA|Makrofotografski otisak|Zelene površine prate pukotine starog građevinskog materijala. Krupni plan otežava procenu razmere i približava prizor apstrakciji.
        Atlas semena|CRTEZ|Tuš i akvarel|Različita semena prikazana su u istoj razmeri i bez ukrasnog okvira. Senke ističu sitne strukture koje omogućavaju njihovo raznošenje.
        Tihi rukavac|SLIKA|Tempera na platnu|Voda gotovo nepomično ponavlja obrise obale. Jedna svetla grana prekida simetriju odraza i uvodi osećaj prolaznosti.
        Portret čitačice|SLIKA|Ulje na platnu|Lice je delimično zaklonjeno knjigom, dok položaj šaka otkriva usredsređenost. Pozadina ostaje mirna kako bi pažnja ostala na gestu.
        Ruke keramičarke|CRTEZ|Ugljen na papiru|Dlanovi su prikazani nakon rada, sa tragovima osušene gline. Tamne linije prate pregibe kože bez idealizovanja površine.
        Komšija na odmoru|SLIKA|Akrilik na platnu|Sedeća figura nije okrenuta posmatraču. Težina tela preneta je preko naslona stolice i spuštenih ramena.
        Autoportret kroz staklo|FOTOGRAFIJA|Kolor pigmentni otisak|Odraz lica preklapa se sa ulicom iza prozora. Dva prostorna plana ostaju podjednako čitljiva i bez jasne granice.
        Devojka sa slušalicama|CRTEZ|Pastel i grafit|Zatvorene oči i blagi nagib glave sugerišu slušanje. Kabl slušalica povezuje lice sa donjom ivicom crteža.
        Portret majstora|SLIKA|Ulje na drvenoj ploči|Figura je okružena samo sa nekoliko pažljivo odabranih alata. Tragovi boje na rukavu govore o poslu više nego poza modela.
        Razgovor bez reči|CRTEZ|Sepija na papiru|Dve figure povezane su smerom pogleda i položajem ruku. Praznina između njih ostavljena je kao aktivan deo razgovora.
        Soba od niti|INSTALACIJA|Pamučni konac i drveni ramovi|Zategnute niti oblikuju prolaz koji se menja sa položajem posmatrača. Preklapanje linija stvara privid gušćih zidova bez zatvaranja prostora.
        Arhiva dodira|INSTALACIJA|Tekstil i odlivci u gipsu|Odlivci udubljenja raspoređeni su uz komade istrošene tkanine. Materijali beleže ponavljanje svakodnevnih dodira bez prikazivanja samih korisnika.
        Mehanizam tišine|INSTALACIJA|Drvo, čelik i opruge|Mali pokret poluge prenosi se kroz nekoliko vidljivih spojeva. Usporena mehanika usmerava pažnju na iščekivanje sledećeg pomeranja.
        Pragovi|INSTALACIJA|Ponovo upotrebljeno drvo|Niz pragova različitih visina označava prelaze između zamišljenih soba. Izlizane površine čuvaju tragove prostora iz kojih su izdvojene.
        Oblak od papira|INSTALACIJA|Presavijen papir i najlon|Lagani elementi vise na različitim visinama. Njihove senke prave drugi, tamniji sloj rada na podu i zidu.
        Zajednički sto|INSTALACIJA|Drvo, keramika i lan|Predmeti različitih autora raspoređeni su oko praznog središta. Rad ispituje kako raspored mesta utiče na mogućnost susreta i razgovora.
        Linije povratka|INSTALACIJA|Čelična žica i tekstilne trake|Zakrivljene putanje vraćaju se prema zajedničkoj tački. Različita zategnutost traka pokazuje da povratak ne znači uvek isti put.
        """);
    // Name | theme | QR code prefix | group | hall index | status | contextual reading | looking prompt
    static final List<String[]> SHOWS = rows("""
        Grad između stanica|Urbana fotografija|PHOTO|0|0|AKTIVNA|Svakodnevno čekanje otkriva grad izvan njegovih reprezentativnih prizora.|Pratite odnos praznog prostora i tragova ljudskog prisustva.
        Ravnoteža materije|Savremena skulptura|SCULPTURE|1|1|AKTIVNA|Težina i oslonac ovde postaju sredstva razgovora između materijala i prostora.|Obiđite delo i uporedite punu formu sa otvorima koji je okružuju.
        Svetlost iza prozora|Svetlo i senka|LIGHT|2|2|AKTIVNA|Promena osvetljenja pretvara poznati enterijer u nestabilnu sliku sećanja.|Potražite prelaz na kojem senka prestaje da opisuje predmet i postaje samostalna površina.
        Otisak susedstva|Grafika i crtež|PRINT|3|3|AKTIVNA|Ponavljanje i mala odstupanja predstavljaju susedstvo kao mrežu ličnih putanja.|Uporedite gustinu tragova sa prazninama koje usmeravaju pogled.
        Predmeti koji traju|Industrijski dizajn|DESIGN|4|4|AKTIVNA|Dobar predmet ovde se razume kroz upotrebu, održavanje i mogućnost popravke.|Pronađite spoj ili detalj koji otkriva kako je predmet napravljen.
        Mala priroda grada|Priroda i gradski život|NATURE|5|5|AKTIVNA|Živi svet na rubovima izgrađenog prostora pokazuje da grad nije odvojen od prirode.|Obratite pažnju na tragove rasta, trošenja i sezonske promene.
        Lica bliskog grada|Portret|PORTRAIT|6|6|PLANIRANA|Portret se čita kao susret sa osobom, a ne kao zbir prepoznatljivih crta.|Posmatrajte šta položaj ruku i tela govori pre nego što se zadržite na licu.
        Prostor za susret|Savremene instalacije|SPACE|7|7|PLANIRANA|Raspored elemenata određuje kako delimo prostor sa drugima.|Zamislite kako se odnos delova menja kada kroz postavku prolazi više ljudi.
        Noćne putanje|Gradski život|URBAN|0|8|PLANIRANA|Grad posle radnog vremena otvara drugačije putanje i načine prisustva.|Pratite granicu između osvetljenog prizora i onoga što ostaje van njega.
        Unutrašnja geometrija|Prostor i forma|FORM|1|0|ZAVRSENA|Skulptura se u ovoj celini čita kao mali arhitektonski prostor.|Pronađite zamišljeni prolaz kroz formu i uporedite ga sa svojim položajem u sali.
        Reka pamti obalu|Priroda i sećanje|RIVER|5|3|ZAVRSENA|Obala postaje arhiva promena koje se ne mogu zadržati u jednom trenutku.|Posmatrajte koji tragovi upućuju na prethodno stanje vode i tla.
        Gestovi svakodnevice|Portret i rad|GESTURE|6|6|ZAVRSENA|Lični gest povezuje prikazanu osobu sa navikama i radom koji je oblikuju.|Uporedite mirne i napete delove prikaza i pokušajte da zamislite pokret koji im prethodi.
        """);
    static String context(int show, int slot) {
        String[] s = SHOWS.get(show), w = WORKS.get(workIndex(show, slot));
        return "Na izložbi „" + s[0] + "“, delo „" + w[0] + "“ uvodi temu „" + s[1].toLowerCase(java.util.Locale.ROOT)
                + "“. " + s[6] + " " + w[3].split("\\. ")[0] + ". " + s[7];
    }
    static int workIndex(int show, int slot) {
        int group = Integer.parseInt(SHOWS.get(show)[3]);
        return slot < 7 ? group * 7 + (slot + show) % 7 : ((group + 1) % 8) * 7 + show % 7;
    }
    static String token(int show, int slot) { return "DEMO-" + SHOWS.get(show)[2] + "-" + "%03d".formatted(slot + 1); }
    static String path(int show, int slot) { return "demo/rich/" + token(show, slot).toLowerCase(java.util.Locale.ROOT) + ".txt"; }
}
