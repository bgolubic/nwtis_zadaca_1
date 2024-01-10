package org.foi.nwtis.bgolubic.zadaca_1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.foi.nwtis.Konfiguracija;
import org.foi.nwtis.KonfiguracijaApstraktna;
import org.foi.nwtis.NeispravnaKonfiguracija;
import org.foi.nwtis.bgolubic.zadaca_1.podaci.MeteoSimulacija;

public class SimulatorMeteo {

  private int ispis;
  private int maksCekanje;
  private int trajanjeSekunde;
  private int brojPokusaja;
  private String korisnickoIme;
  private String korisnickaLozinka;
  private String datotekaProblema;
  private String posluziteljGlavniAdresa;
  private int posluziteljGlavniVrata;

  public static void main(String[] args) {
    var sm = new SimulatorMeteo();
    if (!SimulatorMeteo.provjeriArgumente(args)) {
      Logger.getLogger(PokretacPosluzitelja.class.getName()).log(Level.SEVERE,
          "Nije upisan naziv datoteke ili je dano previše argumenata.");
      return;
    }

    try {
      var konf = sm.ucitajPostavke(args[0]);
      sm.pokreniSimulator(konf);
    } catch (NeispravnaKonfiguracija e) {
      Logger.getLogger(PokretacPosluzitelja.class.getName()).log(Level.SEVERE, e.getMessage());
    } catch (IOException e) {
      Logger.getLogger(PokretacPosluzitelja.class.getName()).log(Level.SEVERE,
          "Greška učitavanja meteo podataka " + e.getMessage());
    }
  }

  private static boolean provjeriArgumente(String[] args) {
    String sintaksa = ".+\\.(txt|xml|bin|json|yaml)";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < args.length; i++) {
      sb.append(args[i]).append(" ");
    }
    String s = sb.toString().trim();
    Pattern uzorak = Pattern.compile(sintaksa);
    Matcher m = uzorak.matcher(s);
    boolean status = m.matches();
    if (status && args.length == 1)
      return true;

    return false;
  }

  Konfiguracija ucitajPostavke(String nazivDatoteke) throws NeispravnaKonfiguracija {
    var konf = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
    spremiPostavke(konf);
    return konf;
  }

  private void spremiPostavke(Konfiguracija konf) {
    this.ispis = Integer.parseInt(konf.dajPostavku("ispis"));
    this.maksCekanje = Integer.parseInt(konf.dajPostavku("maksCekanje"));
    this.trajanjeSekunde = Integer.parseInt(konf.dajPostavku("trajanjeSekunde"));
    this.brojPokusaja = Integer.parseInt(konf.dajPostavku("brojPokusaja"));
    this.korisnickoIme = konf.dajPostavku("korisnickoIme");
    this.korisnickaLozinka = konf.dajPostavku("korisnickaLozinka");
    this.datotekaProblema = konf.dajPostavku("datotekaProblema");
    this.posluziteljGlavniAdresa = konf.dajPostavku("posluziteljGlavniAdresa");
    this.posluziteljGlavniVrata = Integer.parseInt(konf.dajPostavku("posluziteljGlavniVrata"));
  }

  private void pokreniSimulator(Konfiguracija konf) throws IOException {
    var nazivDatoteke = konf.dajPostavku("datotekaMeteo");
    var putanja = Path.of(nazivDatoteke);
    if (!Files.exists(putanja) || Files.isDirectory(putanja) || !Files.isReadable(putanja)) {
      throw new IOException(
          "Datoteka '" + nazivDatoteke + "' nije datoteka ili nije moguće čitati.");
    }
    var citac = Files.newBufferedReader(putanja, Charset.forName("UTF-8"));

    MeteoSimulacija prethodniMeteo = null;

    int rbroj = 0;
    while (true) {
      var red = citac.readLine();
      if (red == null)
        break;

      rbroj++;
      if (isZaglavlje(rbroj))
        continue;

      var atributi = red.split(";");
      if (!redImaPetAtributa(atributi)) {
        Logger.getGlobal().log(Level.WARNING, red);
      } else {
        var vazeciMeteo =
            new MeteoSimulacija(atributi[0], atributi[1], Float.parseFloat(atributi[2]),
                Float.parseFloat(atributi[3]), Float.parseFloat(atributi[4]));
        if (!isPrviPodatak(rbroj)) {
          this.izracunajSpavanje(prethodniMeteo, vazeciMeteo);
        }

        this.posaljiMeteoPodatak(vazeciMeteo, konf);
        prethodniMeteo = vazeciMeteo;
      }
    }
  }

  private boolean isZaglavlje(int rbroj) {
    return rbroj == 1;
  }

  private boolean isPrviPodatak(int rbroj) {
    return rbroj == 2;
  }

  private boolean redImaPetAtributa(String[] atributi) {
    return atributi.length == 5;
  }

  private void posaljiMeteoPodatak(MeteoSimulacija vazeciMeteo, Konfiguracija konf) {
    try {
      var mreznaUticnica = new Socket(posluziteljGlavniAdresa, posluziteljGlavniVrata);
      mreznaUticnica.setSoTimeout(maksCekanje);
      var citac = new BufferedReader(
          new InputStreamReader(mreznaUticnica.getInputStream(), Charset.forName("UTF-8")));
      var pisac = new BufferedWriter(
          new OutputStreamWriter(mreznaUticnica.getOutputStream(), Charset.forName("UTF-8")));
      var poruka = new StringBuilder();
      String zahtjev = generirajKomanduZaPosluzitelj(vazeciMeteo);

      if (this.ispis == 1) {
        Logger.getGlobal().log(Level.INFO, zahtjev);
      }
      pisac.write(zahtjev);
      pisac.flush();
      mreznaUticnica.shutdownOutput();
      while (true) {
        var red = citac.readLine();
        if (red == null)
          break;
        Logger.getGlobal().log(Level.INFO, red);

        poruka.append("RED: " + red);
      }
      Logger.getGlobal().log(Level.INFO, "Odgovor: " + poruka);


      mreznaUticnica.shutdownInput();
      mreznaUticnica.close();
    } catch (IOException e) {
      Logger.getGlobal().log(Level.SEVERE, e.getMessage());
    }
  }

  private String generirajKomanduZaPosluzitelj(MeteoSimulacija meteoSimulacija) {
    StringBuilder sb = new StringBuilder();
    sb.append("KORISNIK " + korisnickoIme + " LOZINKA " + korisnickaLozinka + " SENZOR "
        + meteoSimulacija.id() + " " + meteoSimulacija.vrijeme() + " ");
    if (meteoSimulacija.temperatura() != -999)
      sb.append(meteoSimulacija.temperatura());
    if (meteoSimulacija.vlaga() != -999)
      sb.append(" " + meteoSimulacija.vlaga() + " ");
    if (meteoSimulacija.tlak() != -999)
      sb.append(meteoSimulacija.tlak());
    return sb.toString().trim();
  }

  private void spremiProblem(MeteoSimulacija vazeciMeteo) throws IOException {
    var putanja = Path.of(this.datotekaProblema);
    var tip = Konfiguracija.dajTipKonfiguracije(this.datotekaProblema);
    if (Files.exists(putanja) && (Files.isDirectory(putanja) || !Files.isWritable(putanja))) {
      throw new IOException(
          "Datoteka '" + this.datotekaProblema + "'je direktorij ili nije moguće pisati.");
    }
    try (var output = Files.newOutputStream(putanja)) {
      StringBuilder sb = new StringBuilder();
      sb.append(vazeciMeteo.id() + ";" + vazeciMeteo.vrijeme() + ";" + vazeciMeteo.temperatura()
          + ";" + vazeciMeteo.vlaga() + ";" + vazeciMeteo.tlak());
      output.write(sb.toString().getBytes());
    } catch (IOException e) {
      Logger.getGlobal().log(Level.SEVERE, e.getMessage());
    }

  }

  private void izracunajSpavanje(MeteoSimulacija prethodniMeteo, MeteoSimulacija vazeciMeteo) {
    try {
      String prvi = prethodniMeteo.vrijeme();
      String drugi = vazeciMeteo.vrijeme();
      Date prviVrijeme = new SimpleDateFormat("H:m:s").parse(prvi);
      Date drugiVrijeme = new SimpleDateFormat("H:m:s").parse(drugi);
      int kraj = (int) drugiVrijeme.getTime();
      int pocetak = (int) prviVrijeme.getTime();
      int spavanje = (kraj - pocetak) / 1000;
      spavanje = spavanje * this.trajanjeSekunde;
      if (spavanje > 0)
        Thread.sleep(spavanje);
    } catch (InterruptedException | ParseException e) {
      Logger.getGlobal().log(Level.INFO, e.getMessage());
    }
  }
}
