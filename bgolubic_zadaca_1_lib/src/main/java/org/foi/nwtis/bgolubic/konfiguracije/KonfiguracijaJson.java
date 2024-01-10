package org.foi.nwtis.bgolubic.konfiguracije;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.foi.nwtis.Konfiguracija;
import org.foi.nwtis.KonfiguracijaApstraktna;
import org.foi.nwtis.NeispravnaKonfiguracija;

import com.google.gson.Gson;

/**
 * Klasa konfiguracija za rad s postavkama konfiguracije u json formatu
 * 
 * @author Bruno Golubić
 */

public class KonfiguracijaJson extends KonfiguracijaApstraktna {

  /** Konstanta TIP */
  public static final String TIP = "json";

  /**
   * Konstruktor za inicijalizaciju KonfiguracijeJSON
   * 
   * @param nazivDatoteke - Naziv datoteke
   */
  public KonfiguracijaJson(String nazivDatoteke) {
    super(nazivDatoteke);
    // TODO Auto-generated constructor stub
  }

  @Override
  public void spremiKonfiguraciju(String datoteka) throws NeispravnaKonfiguracija {
    var putanja = Path.of(datoteka);
    var tip = Konfiguracija.dajTipKonfiguracije(datoteka);

    if (tip == null || tip.compareTo(TIP) != 0) {
      throw new NeispravnaKonfiguracija("Datoteka '" + datoteka + "'nije tip " + TIP);
    } else if (Files.exists(putanja)
        && (Files.isDirectory(putanja) || !Files.isWritable(putanja))) {
      throw new NeispravnaKonfiguracija(
          "Datoteka '" + datoteka + "'je direktorij ili nije moguće pisati.");
    }
    try (BufferedWriter out = new BufferedWriter(new FileWriter(datoteka))) {
      Gson gson = new Gson();
      out.write(gson.toJson(this.postavke, Properties.class));
    } catch (IOException e) {
      throw new NeispravnaKonfiguracija(
          "Datoteka '" + datoteka + "'nije moguće upisivati. " + e.getMessage());
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
      throw new NeispravnaKonfiguracija("Datoteka '" + datoteka + "'nije tip " + TIP);
    } else if (Files.exists(putanja)
        && (Files.isDirectory(putanja) || !Files.isReadable(putanja))) {
      throw new NeispravnaKonfiguracija(
          "Datoteka '" + datoteka + "'je direktorij ili nije moguće čitati.");
    }
    try (BufferedReader in = new BufferedReader(new FileReader(datoteka))) {
      Gson gson = new Gson();
      this.postavke = gson.fromJson(in, Properties.class);
    } catch (IOException e) {
      throw new NeispravnaKonfiguracija(
          "Datoteka '" + datoteka + "'nije moguće čitati. " + e.getMessage());
    }
  }

}
