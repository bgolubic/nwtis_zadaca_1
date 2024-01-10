package org.foi.nwtis.bgolubic.konfiguracije;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.foi.nwtis.Konfiguracija;
import org.foi.nwtis.KonfiguracijaApstraktna;
import org.foi.nwtis.NeispravnaKonfiguracija;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.YamlOutputStreamWriter;
import org.snakeyaml.engine.v2.common.ScalarStyle;

/**
 * Klasa konfiguracija za rad s postavkama konfiguracije u yaml formatu
 * 
 * @author Bruno Golubić
 */
public class KonfiguracijaYaml extends KonfiguracijaApstraktna {

  /** Konstanta TIP */
  public static final String TIP = "yaml";

  /**
   * Konstruktor za inicijalizaciju KonfiguracijeYAML
   * 
   * @param nazivDatoteke - Naziv datoteke
   */
  public KonfiguracijaYaml(String nazivDatoteke) {
    super(nazivDatoteke);
  }

  @Override
  public void spremiKonfiguraciju(String datoteka) throws NeispravnaKonfiguracija {
    var putanja = Path.of(datoteka);
    var tip = Konfiguracija.dajTipKonfiguracije(datoteka);

    if (tip == null || tip.compareTo(TIP) != 0) {
      throw new NeispravnaKonfiguracija("Datoteka '" + datoteka + "' nije tip " + TIP);
    } else if (Files.exists(putanja)
        && (Files.isDirectory(putanja) || !Files.isWritable(putanja))) {
      throw new NeispravnaKonfiguracija(
          "Datoteka '" + datoteka + "'je direktorij ili nije moguće pisati.");
    }
    try (YamlOutputStreamWriter pisac = new YamlOutputStreamWriter(Files.newOutputStream(putanja), Charset.forName("UTF-8")) {
      
      @Override
      public void processIOException(IOException e) {
        Logger.getGlobal().log(Level.SEVERE, e.getMessage());
      }
    }){
      DumpSettings yamlPostavke = DumpSettings.builder().setDefaultScalarStyle(ScalarStyle.DOUBLE_QUOTED).build();
      Dump yamlSpremanje = new Dump(yamlPostavke);
      yamlSpremanje.dump(this.postavke, pisac);
    } catch (IOException e) {
      throw new NeispravnaKonfiguracija(
          "Datoteka '" + datoteka + "' nije moguće upisivati. " + e.getMessage());
    }
  }

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
    try (InputStream is = Files.newInputStream(putanja)){
      LoadSettings yamlPostavke = LoadSettings.builder().build();
      Load yamlCitanje = new Load(yamlPostavke);
      this.postavke.putAll((Map<?, ?>) yamlCitanje.loadFromInputStream(is));
    } catch (IOException e) {
      throw new NeispravnaKonfiguracija(
          "Datoteka '" + datoteka + "' nije moguće čitati. " + e.getMessage());
    }
  }

}
