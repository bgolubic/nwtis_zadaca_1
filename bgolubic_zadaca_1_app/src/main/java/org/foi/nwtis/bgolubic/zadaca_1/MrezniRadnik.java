package org.foi.nwtis.bgolubic.zadaca_1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.foi.nwtis.Konfiguracija;
import org.foi.nwtis.bgolubic.zadaca_1.podaci.Korisnik;
import org.foi.nwtis.bgolubic.zadaca_1.podaci.Uredaj;
import org.foi.nwtis.bgolubic.zadaca_1.podaci.UredajVrsta;

/**
 * Klasa MrezniRadnik koja je zadužena za obradu zahtjeva koji stižu na glavni poslužitelj
 * 
 * @author Bruno Golubić
 *
 */
public class MrezniRadnik extends Thread {

  protected Socket mreznaUticnica;
  protected Konfiguracija konfig;
  private int ispis = 0;
  private GlavniPosluzitelj gp;
  private Korisnik korisnik;
  private String odgovor;
  private String alarm = "";
  private boolean aktiviranAlarm = false;

  /**
   * Konstruktor klase u kojem se učitavaju u memoriju podaci iz konfiguracije
   */
  public MrezniRadnik(Socket mreznaUticnica, Konfiguracija konfig, GlavniPosluzitelj gp) {
    this.mreznaUticnica = mreznaUticnica;
    this.konfig = konfig;
    this.ispis = Integer.parseInt(konfig.dajPostavku("ispis"));
    this.gp = gp;
  }

  /**
   * Metoda koja se vrti kada se stvori dretva, ispisuje naziv trenutne dretve
   */
  @Override
  public void start() {
    Logger.getGlobal().log(Level.INFO, "Dretva: " + this.getName());
    super.start();
  }

  /**
   * Metoda koja se vrti nakon pokretanja dretve U ovoj metodi se otvaraju čitač i pisač koji
   * čitaju/pišu podatke preko mrežne utičnice Nakon pročitanih podataka ide se na obradu poslanog
   * zahtjeva
   * 
   * @throws IOException - baca iznimku ako dođe do problema
   */
  @Override
  public void run() {
    try {
      var citac = new BufferedReader(
          new InputStreamReader(this.mreznaUticnica.getInputStream(), Charset.forName("UTF-8")));
      var pisac = new BufferedWriter(
          new OutputStreamWriter(this.mreznaUticnica.getOutputStream(), Charset.forName("UTF-8")));
      var poruka = new StringBuilder();
      while (true) {
        var red = citac.readLine();
        if (red == null)
          break;
        if (this.ispis == 1)
          Logger.getGlobal().log(Level.INFO, red);

        poruka.append(red);
      }
      this.mreznaUticnica.shutdownInput();
      this.obradiZahtjev(poruka.toString());
      Logger.getGlobal().log(Level.INFO, "Odgovor: " + odgovor);
      pisac.write(odgovor);
      pisac.flush();
      this.mreznaUticnica.shutdownOutput();
      this.mreznaUticnica.close();
      this.interrupt();
    } catch (IOException e) {
      Logger.getGlobal().log(Level.SEVERE, e.getMessage());
    }
  }

  /**
   * Metoda u kojoj se provjerava korisnik i ako je u redu kreće se s obradom predmetnog dijela
   * komande
   */
  public void obradiZahtjev(String komanda) {
    String autentikacijskiDio = dohvatiAutentikacijskiDio(komanda);
    if (obradiAutentikacijskiDio(autentikacijskiDio)) {
      String predmetniDio = dohvatiPredmetniDio(komanda);
      obradiPredmetniDio(predmetniDio);
    } else {
      this.odgovor =
          "ERROR 21 Greška kod autentikacije, provjerite korisničko ime/lozinku i pokušajte ponovo";
    }
  }

  /**
   * Metoda u kojoj se dohvaća autentikacijski dio komande
   */
  private String dohvatiAutentikacijskiDio(String komanda) {
    String[] podjela = komanda.split("\\s+");
    StringBuilder sb = new StringBuilder();
    sb.append(podjela[1] + ";" + podjela[3]);
    return sb.toString();
  }

  /**
   * Metoda u kojoj se korisničko ime i korisnička lozinka
   */
  private boolean obradiAutentikacijskiDio(String autentikacijskiDio) {
    String[] korisnickiPodaci = autentikacijskiDio.split(";");
    korisnik = gp.korisnici.get(korisnickiPodaci[0]);

    if (korisnik != null && korisnik.lozinka().equals(korisnickiPodaci[1]))
      return true;

    return false;
  }

  /**
   * Metoda u kojoj se dohvaća predmetni dio komande
   */
  private String dohvatiPredmetniDio(String komanda) {
    String[] podjela = komanda.split("\\s+");
    StringBuilder sb = new StringBuilder();
    for (int i = 4; i < podjela.length; i++) {
      sb.append(podjela[i].toString() + " ");
    }
    return sb.toString().trim();
  }

  /**
   * Metoda u kojoj se obrađuje predmetni dio komande
   */
  private void obradiPredmetniDio(String predmetniDio) {
    if (!korisnik.administrator()
        && (predmetniDio.endsWith("KRAJ") || predmetniDio.startsWith("SENZOR"))) {
      odgovor = "ERROR 22 Korisnik nije administrator";
      return;
    }

    if (predmetniDio.endsWith("KRAJ")) {
      odgovor = "OK";
      gp.kraj = true;
      gp.ugasiPosluzitelj();
    }

    if (predmetniDio.startsWith("SENZOR"))
      odgovor = obradiSenzor(predmetniDio.substring(7));

    if (predmetniDio.startsWith("METEO"))
      odgovor = obradiMeteo(predmetniDio.substring(6));

    if (predmetniDio.startsWith("MAKS"))
      odgovor = obradiMaks(predmetniDio.substring(5));

    if (predmetniDio.startsWith("ALARM"))
      odgovor = obradiAlarm(predmetniDio.substring(6).replace("'", ""));

    if (predmetniDio.startsWith("UDALJENOST")) {
      odgovor = obradiUdaljenost(predmetniDio);
    }

  }

  /**
   * Metoda u kojoj se obrađuje komanda SENZOR
   */
  private String obradiSenzor(String podaci) {
    StringBuilder sb = new StringBuilder();
    sb.append("OK");
    String[] podijeljeniPodaci = podaci.split("\\s+");
    var idUredaj = podijeljeniPodaci[0];
    var brojPrimljenihPodataka = podijeljeniPodaci.length - 2;

    var uredaj = gp.uredaji.get(idUredaj);

    if (uredaj == null)
      return "ERROR 23 Ne postoji uređaj s tim ID-em";

    if (!((uredaj.vrsta() == UredajVrsta.SenzorTemperatura) && (brojPrimljenihPodataka == 1))
        && !((uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlaga)
            && (brojPrimljenihPodataka == 2))
        && !((uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlagaTlak)
            && (brojPrimljenihPodataka == 3)))
      return "ERROR 29 Vrsta uređaja ne odgovara broju primljenih podataka";

    if (uredaj.vrsta() == UredajVrsta.SenzorTemperatura
        || uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlaga
        || uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlagaTlak)
      spremiTemp(uredaj, podijeljeniPodaci[2]);


    if (uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlaga
        || uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlagaTlak)
      spremiVlaga(uredaj, podijeljeniPodaci[3]);


    if (uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlagaTlak)
      spremiTlak(uredaj, podijeljeniPodaci[4]);

    this.gp.vremenaZadnjihOcitanja.put(uredaj.id(), podijeljeniPodaci[1]);

    if (this.aktiviranAlarm) {
      sb.append(" ALARM " + alarm);
      this.gp.alarmi.put(uredaj.idLokacija(),
          uredaj.id() + " " + podijeljeniPodaci[1] + " " + sb.substring(3).trim());
    }

    return sb.toString().trim();
  }

  /**
   * Metoda u kojoj se sprema temperatura proslijeđena u komandi SENZOR
   */
  private void spremiTemp(Uredaj uredaj, String temp) {
    List<Float> ocitanja = this.gp.ocitanjaTemp.get(uredaj.id());
    if (ocitanja == null) {
      var pocetnoOcitanje = new LinkedList<Float>();
      pocetnoOcitanje.add(Float.parseFloat(temp));
      this.gp.ocitanjaTemp.put(uredaj.id(), pocetnoOcitanje);
    } else {
      for (var podatak : ocitanja) {
        var razlika = Math.abs(podatak - Double.parseDouble(temp));
        if (razlika > Float.parseFloat(this.konfig.dajPostavku("odstupanjeTemp"))) {
          ocitanja.clear();
          aktiviranAlarm = true;
          alarm = alarm.concat("TEMP ");
          break;
        }

      }

      ocitanja.add(Float.parseFloat(temp));
    }

  }

  /**
   * Metoda u kojoj se sprema vlaga proslijeđena u komandi SENZOR
   */
  private void spremiVlaga(Uredaj uredaj, String vlaga) {
    List<Float> ocitanja = this.gp.ocitanjaVlaga.get(uredaj.id());
    if (ocitanja == null) {
      var pocetnoOcitanje = new LinkedList<Float>();
      pocetnoOcitanje.add(Float.parseFloat(vlaga));
      this.gp.ocitanjaVlaga.put(uredaj.id(), pocetnoOcitanje);
    } else {
      for (var podatak : ocitanja) {
        var razlika = Math.abs(podatak - Float.parseFloat(vlaga));
        if (razlika > Float.parseFloat(this.konfig.dajPostavku("odstupanjeVlaga"))) {
          ocitanja.clear();
          aktiviranAlarm = true;
          alarm = alarm.concat("VLAGA ");
          break;
        }

      }

      ocitanja.add(Float.parseFloat(vlaga));
    }

  }

  /**
   * Metoda u kojoj se sprema tlak proslijeđen u komandi SENZOR
   */
  private void spremiTlak(Uredaj uredaj, String tlak) {
    List<Float> ocitanja = this.gp.ocitanjaTlak.get(uredaj.id());
    if (ocitanja == null) {
      var pocetnoOcitanje = new LinkedList<Float>();
      pocetnoOcitanje.add(Float.parseFloat(tlak));
      this.gp.ocitanjaTlak.put(uredaj.id(), pocetnoOcitanje);
    } else {
      for (var podatak : ocitanja) {
        var razlika = Math.abs(podatak - Double.parseDouble(tlak));
        if (razlika > Float.parseFloat(this.konfig.dajPostavku("odstupanjeTlak"))) {
          ocitanja.clear();
          aktiviranAlarm = true;
          alarm = alarm.concat("TLAK");
          break;
        }

      }

      ocitanja.add(Float.parseFloat(tlak));
    }

  }

  /**
   * Metoda u kojoj se obrađuje komanda METEO
   */
  private String obradiMeteo(String idUredaj) {
    StringBuilder sb = new StringBuilder();
    sb.append("OK");

    var uredaj = gp.uredaji.get(idUredaj);

    if (uredaj == null)
      return "ERROR 23 Ne postoji uređaj s tim ID-em";

    if (postojePodaciZaUredaj(uredaj)) {
      sb.append(" " + this.gp.vremenaZadnjihOcitanja.get(uredaj.id()));
      if (uredaj.vrsta() == UredajVrsta.SenzorTemperatura
          || uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlaga
          || uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlagaTlak)
        sb.append(" " + this.gp.ocitanjaTemp.get(uredaj.id()).getLast());


      if (uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlaga
          || uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlagaTlak)
        sb.append(" " + this.gp.ocitanjaVlaga.get(uredaj.id()).getLast());


      if (uredaj.vrsta() == UredajVrsta.SenzorTemperaturaVlagaTlak)
        sb.append(" " + this.gp.ocitanjaTlak.get(uredaj.id()).getLast());
    }

    return sb.toString();
  }

  /**
   * Metoda u kojoj se provjerava postoje li podaci za uređaj naveden u komandi METEO
   */
  private boolean postojePodaciZaUredaj(Uredaj uredaj) {
    if (this.gp.ocitanjaTemp.containsKey(uredaj.id())
        || this.gp.ocitanjaVlaga.containsKey(uredaj.id())
        || this.gp.ocitanjaTlak.containsKey(uredaj.id()))
      return true;
    return false;
  }

  /**
   * Metoda u kojoj se obrađuje komanda MAKS
   */
  private String obradiMaks(String podaci) {
    StringBuilder sb = new StringBuilder();
    sb.append("OK");

    String[] podijeljeniPodaci = podaci.split("\\s+");
    var vrsta = podijeljeniPodaci[0];
    var uredaj = this.gp.uredaji.get(podijeljeniPodaci[1]);

    if (uredaj == null)
      return "ERROR 23 Ne postoji uređaj s tim ID-em";

    if (postojePodaciZaUredaj(uredaj)) {
      sb.append(" " + this.gp.vremenaZadnjihOcitanja.get(uredaj.id()));
      if (vrsta.equals("TEMP"))
        sb.append(" " + Collections.max(this.gp.ocitanjaTemp.get(uredaj.id())));

      if (vrsta.equals("VLAGA"))
        sb.append(" " + Collections.max(this.gp.ocitanjaVlaga.get(uredaj.id())));

      if (vrsta.equals("TLAK"))
        sb.append(" " + Collections.max(this.gp.ocitanjaTlak.get(uredaj.id())));
    }

    return sb.toString();
  }

  /**
   * Metoda u kojoj se obrađuje komanda ALARM
   */
  private String obradiAlarm(String idLokacije) {
    StringBuilder sb = new StringBuilder();
    sb.append("OK");

    var podaciLokacije = this.gp.alarmi.get(idLokacije);

    if (podaciLokacije == null)
      return "ERROR 24 Ne postoji alarm na ovoj lokaciji";

    sb.append(" " + podaciLokacije);

    return sb.toString();
  }

  /**
   * Metoda u kojoj se obrađuje komanda UDALJENOST
   */
  public String obradiUdaljenost(String komanda) {
    StringBuilder sb = new StringBuilder();
    String[] podijeljenjaKomanda = komanda.split("\\s+");
    if (komanda.contains("SPREMI")) {
      sb.append(posaljiZahtjev(podijeljenjaKomanda[0] + " " + podijeljenjaKomanda[1]));
    } else {
      var lokacija1 =
          podijeljenjaKomanda[1].replace("'", "") + " " + podijeljenjaKomanda[2].replace("'", "");
      var lokacija2 =
          podijeljenjaKomanda[3].replace("'", "") + " " + podijeljenjaKomanda[4].replace("'", "");

      if (!this.gp.lokacije.containsKey(lokacija1) || !this.gp.lokacije.containsKey(lokacija2))
        return "ERROR 24 Ne postoji jedna od danih lokacija";

      var gpsSirina1 = dohvatiSirinu(lokacija1);
      var gpsSirina2 = dohvatiSirinu(lokacija2);
      var gpsDuzina1 = dohvatiDuzinu(lokacija1);
      var gpsDuzina2 = dohvatiDuzinu(lokacija2);

      sb.append(posaljiZahtjev(podijeljenjaKomanda[0] + " " + gpsSirina1 + " " + gpsDuzina1 + " "
          + gpsSirina2 + " " + gpsDuzina2));
    }



    return sb.toString();
  }

  /**
   * Metoda u kojoj se šalje zahtjev na poslužitelj udaljenosti
   */
  public String posaljiZahtjev(String komanda) {
    try {
      if (this.gp.otvoriMreznaVrataPosluziteljaUdaljenosti()) {
        var citac = new BufferedReader(new InputStreamReader(
            this.gp.mreznaUticnicaUdaljenosti.getInputStream(), Charset.forName("UTF-8")));
        var pisac = new BufferedWriter(new OutputStreamWriter(
            this.gp.mreznaUticnicaUdaljenosti.getOutputStream(), Charset.forName("UTF-8")));
        var poruka = new StringBuilder();
        String zahtjev = komanda;
        pisac.write(zahtjev);
        pisac.flush();
        this.gp.mreznaUticnicaUdaljenosti.shutdownOutput();
        while (true) {
          var red = citac.readLine();
          if (red == null)
            break;
          poruka.append("RED: " + red);
        }
        this.gp.mreznaUticnicaUdaljenosti.shutdownInput();
        this.gp.mreznaUticnicaUdaljenosti.close();
        return poruka.toString();
      }
    } catch (IOException e) {
      Logger.getGlobal().log(Level.SEVERE, e.getMessage());
    }
    return "Mrežna vrata poslužietalja udaljenosti su zatvorena";
  }

  /**
   * Metoda u kojoj se dohvaća geografska širina uređaja
   */
  public String dohvatiSirinu(String idLokacije) {
    var lokacija = this.gp.lokacije.get(idLokacije);
    return lokacija.gpsSirina();
  }

  /**
   * Metoda u kojoj se dohvaća geografska dužina uređaja
   */
  public String dohvatiDuzinu(String idLokacije) {
    var lokacija = this.gp.lokacije.get(idLokacije);
    return lokacija.gpsDuzina();
  }

  /**
   * Metoda koja se odrađuje na kraju života dretve, smanjuje se broj aktivnih dretvi i gasi se
   * poslužitelj ako je zadovoljen uvjet
   */
  @Override
  public void interrupt() {
    this.gp.brojAktivnihDretvi--;
    if (this.gp.kraj)
      this.gp.ugasiPosluzitelj();
    super.interrupt();
  }

}
