package org.foi.nwtis.bgolubic.konfiguracije;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.foi.nwtis.Konfiguracija;
import org.foi.nwtis.KonfiguracijaApstraktna;
import org.foi.nwtis.NeispravnaKonfiguracija;

/**
 * Klasa konfiguracija za rad s postavkama konfiguracije u xml formatu
 * 
 * @author Bruno Golubić
 */
public class KonfiguracijaXml extends KonfiguracijaApstraktna {

  /** Konstanta TIP */
  public static final String TIP = "xml";

  /**
   * Konstruktor za inicijalizaciju KonfiguracijeXML
   * 
   * @param nazivDatoteke - Naziv datoteke
   */
  public KonfiguracijaXml(String nazivDatoteke) {
    super(nazivDatoteke);
    // TODO Auto-generated constructor stub
  }

  /**
   * Sprema konfiguraciju na disk.
   * 
   * @param datoteka - Naziv datoteke
   * @throws NeispravnaKonfiguracija - iznimka kada nešto ne valja
   */
  @Override
  public void spremiKonfiguraciju(String datoteka) throws NeispravnaKonfiguracija {
    var putanja = Path.of(datoteka);
    var tip = Konfiguracija.dajTipKonfiguracije(datoteka);

    if (tip == null || tip.compareTo(TIP) != 0) {
      throw new NeispravnaKonfiguracija("Datoteka '" + datoteka + "' nije tip " + TIP);
    } else if (Files.exists(putanja)
        && (Files.isDirectory(putanja) || !Files.isWritable(putanja))) {
      throw new NeispravnaKonfiguracija(
          "Datoteka '" + datoteka + "' je direktorij ili nije moguće pisati.");
    }
    try {
      this.postavke.storeToXML(Files.newOutputStream(putanja), "NWTiS bgolubic 2023.");
    } catch (IOException e) {
      throw new NeispravnaKonfiguracija(
          "Datoteka '" + datoteka + "' nije moguće upisivati. " + e.getMessage());
    }

  }

  /**
   * Učitaj konfiguraciju s diska
   * 
   * @throws NeispravnaKonfiguracija - ako nije ispravna
   */
  @Override
  public void ucitajKonfiguraciju() throws NeispravnaKonfiguracija {
    var datoteka = this.nazivDatoteke;
    var putanja = Path.of(datoteka);
    var tip = Konfiguracija.dajTipKonfiguracije(datoteka);

    if (tip == null || tip.compareTo(TIP) != 0) {
      throw new NeispravnaKonfiguracija("Datoteka '" + datoteka + "' nije tip " + TIP);
    } else if (Files.exists(putanja)
        && (Files.isDirectory(putanja) || !Files.isReadable(putanja))) {
      throw new NeispravnaKonfiguracija(
          "Datoteka '" + datoteka + "' je direktorij ili nije moguće čitati.");
    }
    try {
      this.postavke.loadFromXML(Files.newInputStream(putanja));
    } catch (IOException e) {
      throw new NeispravnaKonfiguracija(
          "Datoteka '" + datoteka + "' nije moguće čitati. " + e.getMessage());
    }

  }

}
