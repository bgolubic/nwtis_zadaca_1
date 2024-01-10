package org.foi.nwtis.bgolubic.zadaca_1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.foi.nwtis.Konfiguracija;
import org.foi.nwtis.KonfiguracijaApstraktna;
import org.foi.nwtis.NeispravnaKonfiguracija;

public class PosluziteljUdaljenosti {

  private Konfiguracija konf;
  private int ispis;
  private int mreznaVrata;
  private int brojCekaca;
  private int brojZadnjihSpremljenih;
  private ServerSocket posluzitelj;
  private LinkedHashMap<String, String> zahtjevi = new LinkedHashMap<String, String>();

  public static void main(String[] args) {
    var pu = new PosluziteljUdaljenosti();
    if (!pu.provjeriArgumente(args)) {
      Logger.getLogger(PosluziteljUdaljenosti.class.getName()).log(Level.SEVERE,
          "Nije upisan naziv datoteke ili je dano previše argumenata.");
      return;
    }
    try {
      pu.konf = pu.ucitajKonfiguraciju(args[0]);
      pu.ucitajPodatke();
      pu.otvoriMreznaVrata();
    } catch (NeispravnaKonfiguracija e) {
      Logger.getLogger(PosluziteljUdaljenosti.class.getName()).log(Level.SEVERE,
          "Pogreška kod učitavanja postavki iz datoteke! " + e.getMessage());
    } catch (IOException e) {
      Logger.getGlobal().log(Level.SEVERE, e.getMessage());
    }
  }

  private boolean provjeriArgumente(String[] args) {
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

  Konfiguracija ucitajKonfiguraciju(String nazivDatoteke) throws NeispravnaKonfiguracija {
    return KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
  }

  private void ucitajPodatke() throws IOException {
    this.ispis = Integer.parseInt(this.konf.dajPostavku("ispis"));
    this.mreznaVrata = Integer.parseInt(this.konf.dajPostavku("mreznaVrata"));
    this.brojCekaca = Integer.parseInt(this.konf.dajPostavku("brojCekaca"));
    this.brojZadnjihSpremljenih = Integer.parseInt(this.konf.dajPostavku("brojZadnjihSpremljenih"));
    var datoteka = this.konf.dajPostavku("datotekaSerijalizacija");
    if (Files.exists(Path.of(datoteka)))
      this.zahtjevi = ucitajDatoteku(datoteka);
    else
      Logger.getGlobal().log(Level.SEVERE, "Ne postoji datoteka definirana u konfiguraciji");
  }

  public LinkedHashMap<String, String> ucitajDatoteku(String nazivDatoteke) throws IOException {
    var putanja = Path.of(nazivDatoteke);
    if (!Files.exists(putanja) || Files.isDirectory(putanja) || !Files.isReadable(putanja)) {
      throw new IOException(
          "Datoteka '" + nazivDatoteke + "' nije datoteka ili nije moguće čitati.");
    }

    var zahtjevi = new LinkedHashMap<String, String>();

    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(nazivDatoteke))) {
      zahtjevi = (LinkedHashMap<String, String>) ois.readObject();
    } catch (ClassNotFoundException e) {
      Logger.getGlobal().log(Level.SEVERE, e.getMessage());
    }

    return zahtjevi;
  }

  public void otvoriMreznaVrata() {
    try {
      this.posluzitelj = new ServerSocket(this.mreznaVrata, this.brojCekaca);
      while (true) {
        var uticnica = this.posluzitelj.accept();
        posaljiZahtjev(uticnica);
      }

    } catch (IOException e) {
      Logger.getGlobal().log(Level.SEVERE, e.getMessage());
    } finally {
      try {
        this.posluzitelj.close();
      } catch (IOException e) {
        Logger.getGlobal().log(Level.INFO, "Poslužitelj ugašen");
      }
    }
  }

  private void posaljiZahtjev(Socket mreznaUticnica) {
    try {
      var citac = new BufferedReader(
          new InputStreamReader(mreznaUticnica.getInputStream(), Charset.forName("UTF-8")));
      var pisac = new BufferedWriter(
          new OutputStreamWriter(mreznaUticnica.getOutputStream(), Charset.forName("UTF-8")));
      var poruka = new StringBuilder();
      while (true) {
        var red = citac.readLine();
        if (red == null)
          break;
        if (this.ispis == 1)
          Logger.getGlobal().log(Level.INFO, red);

        poruka.append(red);
      }
      mreznaUticnica.shutdownInput();
      var odgovor = this.obradiZahtjev(poruka.toString());
      Logger.getGlobal().log(Level.INFO, "Odgovor: " + odgovor);
      pisac.write(odgovor);
      pisac.flush();
      mreznaUticnica.shutdownOutput();
      mreznaUticnica.close();
    } catch (IOException e) {
      Logger.getGlobal().log(Level.SEVERE, e.getMessage());
    }
  }

  private String obradiZahtjev(String zahtjev) throws IOException {
    StringBuilder sb = new StringBuilder();
    String[] podijeljeniZahtjev = zahtjev.split("\\s+");
    if (podijeljeniZahtjev.length == 2) {
      sb.append(spremiPodatke(this.konf.dajPostavku("datotekaSerijalizacija")));
    } else if (podijeljeniZahtjev.length == 5) {
      var spremljeniZahtjev = this.zahtjevi.get(zahtjev);
      if (spremljeniZahtjev != null)
        sb.append("OK " + spremljeniZahtjev);

      else {
        var udaljenost = izracunajUdaljenost(podijeljeniZahtjev[1], podijeljeniZahtjev[2],
            podijeljeniZahtjev[3], podijeljeniZahtjev[4]);
        if (this.zahtjevi.size() == this.brojZadnjihSpremljenih) {
          for (var zapis : this.zahtjevi.entrySet()) {
            this.zahtjevi.remove(zapis.getKey());
            break;
          }
        } else if ((this.zahtjevi.size() > this.brojZadnjihSpremljenih)
            && this.brojZadnjihSpremljenih != 1) {
          ArrayList<String> kljuceviZaBrisanje = new ArrayList<>();
          int razlika = this.zahtjevi.size() - this.brojZadnjihSpremljenih;
          int brojac = 0;
          for (var zapis : this.zahtjevi.entrySet()) {
            if (brojac == razlika)
              break;
            kljuceviZaBrisanje.add(zapis.getKey());
            brojac++;
          }
          for (var zapis : kljuceviZaBrisanje)
            this.zahtjevi.remove(zapis);
        } else if (this.brojZadnjihSpremljenih == 1)
          this.zahtjevi.clear();
        this.zahtjevi.put(zahtjev, udaljenost);
        sb.append("OK " + udaljenost);
      }
    }
    return sb.toString();
  }

  public String spremiPodatke(String nazivDatoteke) throws IOException {
    var putanja = Path.of(nazivDatoteke);

    if (Files.exists(putanja) && (Files.isDirectory(putanja) || !Files.isWritable(putanja))) {
      throw new IOException(
          "Datoteka '" + nazivDatoteke + "'je direktorij ili nije moguće pisati.");
    }
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(nazivDatoteke))) {
      oos.writeObject(this.zahtjevi);
    }

    return "OK";
  }

  private String izracunajUdaljenost(String gpsSirina1, String gpsDuzina1, String gpsSirina2,
      String gpsDuzina2) {

    float sirina1 = Float.parseFloat(gpsSirina1);
    float sirina2 = Float.parseFloat(gpsSirina2);
    float duzina1 = Float.parseFloat(gpsDuzina1);
    float duzina2 = Float.parseFloat(gpsDuzina2);
    if (((sirina1 < -90 || sirina1 > 90) || (sirina2 < -90 || sirina2 > 90))
        || ((duzina1 < -180 || duzina1 > 180) || (duzina2 < -180 || duzina2 > 180)))
      return "Širina/dužina je neispravna.";

    double earthRadius = 6371000;
    double dLat = Math.toRadians(sirina2 - sirina1);
    double dLng = Math.toRadians(duzina2 - duzina1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(sirina1))
        * Math.cos(Math.toRadians(sirina2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    float dist = (float) (earthRadius * c);
    dist = dist / 1000;

    return String.valueOf(dist);
  }
}
