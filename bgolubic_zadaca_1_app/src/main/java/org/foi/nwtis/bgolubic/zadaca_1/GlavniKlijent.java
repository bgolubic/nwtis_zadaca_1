package org.foi.nwtis.bgolubic.zadaca_1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlavniKlijent {
  private LinkedHashMap<String, String> argumenti = new LinkedHashMap<String, String>();
  private String adresa;
  private int mreznaVrata;
  private int maksCekanje;
  private String komanda;

  public static void main(String[] args) {
    var gk = new GlavniKlijent();
    if (!gk.provjeriArgumente(args)) {
      Logger.getGlobal().log(Level.SEVERE, "Nisu ispravni ulazni argumenti!");
      return;
    }

    gk.spojiSeNaPosluzitelj(gk.adresa, gk.mreznaVrata);
  }

  private boolean provjeriArgumente(String[] args) {
    String sintaksa = "-k (?<korisnik>[a-zA-Z0-9_-]{3,10}) -l (?<lozinka>[a-zA-Z0-9_\\-#!]{3,10}) "
        + "-a ((?<ipadresa>\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b)"
        + "|(?<adresa>[a-z\\.]+)) -v (?<mreznaVrata>(8\\d{3}|9\\d{3})) -t (?<cekanje>[0-9]+) "
        + "(((--meteo (?<meteo>[A-ZŠĐČĆŽ0-9]+\\-[A-ZŠĐČĆŽ0-9]+))|(--makstemp (?<makstemp>[A-ZŠĐČĆŽ0-9]+\\-[A-ZŠĐČĆŽ0-9]+))"
        + "|(--maksvlaga (?<maksvlaga>[A-ZŠĐČĆŽ0-9]+\\-[A-ZŠĐČĆŽ0-9]+))|(--makstlak (?<makstlak>[A-ZŠĐČĆŽ0-9]+\\-[A-ZŠĐČĆŽ0-9]+)))"
        + "|(--alarm (?<alarm>'[A-ZŠĐČĆŽ0-9]+\\ [A-ZŠĐČĆŽ0-9]+'))|(--udaljenost (?<udaljenost>('[A-ZŠĐČĆŽ0-9]+\\ [A-ZŠĐČĆŽ0-9]+'\\"
        + " '[A-ZŠĐČĆŽ0-9]+\\ [A-ZŠĐČĆŽ0-9]+')|(spremi)))|(?<kraj>--kraj))";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < args.length; i++) {
      sb.append(args[i]).append(" ");
    }
    String s = sb.toString().trim();
    Pattern uzorak = Pattern.compile(sintaksa);
    Matcher m = uzorak.matcher(s);
    boolean status = m.matches();
    if (status) {
      spremiArgumente(m);
      return true;
    }

    return false;
  }

  private void spojiSeNaPosluzitelj(String adresa, Integer mreznaVrata) {
    try {
      var mreznaUticnica = new Socket(adresa, mreznaVrata);
      mreznaUticnica.setSoTimeout(this.maksCekanje);
      var citac = new BufferedReader(
          new InputStreamReader(mreznaUticnica.getInputStream(), Charset.forName("UTF-8")));
      var pisac = new BufferedWriter(
          new OutputStreamWriter(mreznaUticnica.getOutputStream(), Charset.forName("UTF-8")));
      var poruka = new StringBuilder();
      String zahtjev = this.komanda;
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

  private void spremiArgumente(Matcher m) {
    argumenti.put("KORISNIK", m.group("korisnik"));
    argumenti.put("LOZINKA", m.group("lozinka"));
    adresa = m.group("ipadresa") != "null" ? m.group("ipadresa") : m.group("adresa");
    mreznaVrata = Integer.parseInt(m.group("mreznaVrata"));
    maksCekanje = Integer.parseInt(m.group("cekanje"));
    argumenti.put("METEO", m.group("meteo"));
    argumenti.put("MAKS TEMP", m.group("makstemp"));
    argumenti.put("MAKS VLAGA", m.group("maksvlaga"));
    argumenti.put("MAKS TLAK", m.group("makstlak"));
    argumenti.put("ALARM", m.group("alarm"));
    if (m.group("udaljenost") != null && m.group("udaljenost").equals("spremi"))
      argumenti.put("UDALJENOST", m.group("udaljenost").toUpperCase());
    else
      argumenti.put("UDALJENOST", m.group("udaljenost"));
    argumenti.put("KRAJ", m.group("kraj"));
    generirajKomanduZaPosluzitelj();
  }

  private void generirajKomanduZaPosluzitelj() {
    StringBuilder sb = new StringBuilder();
    for (var zapis : argumenti.entrySet()) {
      if (zapis.getValue() != null) {
        if (zapis.getKey() != "KRAJ")
          sb.append(zapis.getKey() + " " + zapis.getValue() + " ");
        else
          sb.append(zapis.getKey());
      }
    }
    this.komanda = sb.toString().trim();
  }
}
