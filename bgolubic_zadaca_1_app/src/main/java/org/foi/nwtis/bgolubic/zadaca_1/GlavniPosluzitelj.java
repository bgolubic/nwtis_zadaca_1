package org.foi.nwtis.bgolubic.zadaca_1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.foi.nwtis.Konfiguracija;
import org.foi.nwtis.KonfiguracijaApstraktna;
import org.foi.nwtis.NeispravnaKonfiguracija;
import org.foi.nwtis.bgolubic.zadaca_1.podaci.Korisnik;
import org.foi.nwtis.bgolubic.zadaca_1.podaci.Lokacija;
import org.foi.nwtis.bgolubic.zadaca_1.podaci.Uredaj;
import org.foi.nwtis.bgolubic.zadaca_1.pomocnici.CitanjeKorisnika;
import org.foi.nwtis.bgolubic.zadaca_1.pomocnici.CitanjeLokacija;
import org.foi.nwtis.bgolubic.zadaca_1.pomocnici.CitanjeUredaja;

/**
 * Klasa GlavniPosluzitelj koja je zadužena za otvaranje veze na određenim mrežnim vratima/portu
 * 
 * @author Bruno Golubić
 *
 */
public class GlavniPosluzitelj {

  protected Konfiguracija konf;
  protected Map<String, Korisnik> korisnici;
  protected Map<String, Lokacija> lokacije;
  protected Map<String, Uredaj> uredaji;
  protected Map<String, LinkedList<Float>> ocitanjaTemp = new HashMap<String, LinkedList<Float>>();
  protected Map<String, LinkedList<Float>> ocitanjaVlaga = new HashMap<String, LinkedList<Float>>();
  protected Map<String, LinkedList<Float>> ocitanjaTlak = new HashMap<String, LinkedList<Float>>();
  protected Map<String, String> vremenaZadnjihOcitanja = new HashMap<String, String>();
  protected Map<String, String> alarmi = new HashMap<String, String>();
  private int ispis;
  private int maksCekanje;
  private int mreznaVrata;
  private int brojCekaca;
  private int brojRadnika;
  private String posluziteljUdaljenostiAdresa;
  private int posluziteljUdaljenostiVrata;
  private ServerSocket posluzitelj;
  protected int brojAktivnihDretvi = 0;
  protected boolean kraj = false;
  protected Socket mreznaUticnicaUdaljenosti;

  /**
   * Konstruktor klase u kojem se učitavaju u memoriju podaci iz konfiguracije
   */
  public GlavniPosluzitelj(String datotekaKonfiguracije) {
    try {
      this.konf = ucitajPostavke(datotekaKonfiguracije);
    } catch (NeispravnaKonfiguracija e) {
      Logger.getLogger(PokretacPosluzitelja.class.getName()).log(Level.SEVERE,
          "Pogreška kod učitavanja postavki iz datoteke! " + e.getMessage());
    }
    this.ispis = Integer.parseInt(konf.dajPostavku("ispis"));
    this.maksCekanje = Integer.parseInt(konf.dajPostavku("maksCekanje"));
    this.mreznaVrata = Integer.parseInt(konf.dajPostavku("mreznaVrata"));
    this.brojCekaca = Integer.parseInt(konf.dajPostavku("brojCekaca"));
    this.brojRadnika = Integer.parseInt(konf.dajPostavku("brojRadnika"));
    this.posluziteljUdaljenostiAdresa = konf.dajPostavku("posluziteljUdaljenostiAdresa");
    this.posluziteljUdaljenostiVrata =
        Integer.parseInt(konf.dajPostavku("posluziteljUdaljenostiVrata"));
  }

  /**
   * Metoda za učitavanje postavki iz konfiguracije
   * 
   * @throws NeispravnaKonfiguracija
   */
  Konfiguracija ucitajPostavke(String nazivDatoteke) throws NeispravnaKonfiguracija {
    return KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
  }

  /**
   * Metoda koja poziva metode za učitavanje podataka i otvaranje mrežnih vrata
   * 
   * @throws IOException - baca iznimku ako dođe do problema
   */
  public void pokreniPosluzitelj() {
    try {
      Logger.getGlobal().log(Level.INFO, "Poslužitelj upaljen");
      ucitajKorisnike();
      ucitajLokacije();
      ucitajUredaje();
      otvoriMreznaVrata();
    } catch (IOException e) {
      Logger.getGlobal().log(Level.SEVERE, e.getMessage());
    }
  }

  /**
   * Učitava sve korisnike iz CSV datoteke koja je definirana u postavci datotekaKorisnika
   * 
   * @throws IOException - baca iznimku ako je problem s učitavanjem
   */
  public void ucitajKorisnike() throws IOException {
    var nazivDatoteke = this.konf.dajPostavku("datotekaKorisnika");
    var citacKorisnika = new CitanjeKorisnika();
    this.korisnici = citacKorisnika.ucitajDatoteku(nazivDatoteke);
    if (this.ispis == 1) {
      for (String korIme : this.korisnici.keySet()) {
        var korisnik = this.korisnici.get(korIme);
        Logger.getGlobal().log(Level.INFO,
            "Korisnik: " + korisnik.prezime() + " " + korisnik.ime());
      }
    }
  }

  /**
   * Učitava sve lokacije iz CSV datoteke koja je definirana u postavci datotekaLokacija
   * 
   * @throws IOException - baca iznimku ako je problem s učitavanjem
   */
  public void ucitajLokacije() throws IOException {
    var nazivDatoteke = this.konf.dajPostavku("datotekaLokacija");
    var citacLokacija = new CitanjeLokacija();
    this.lokacije = citacLokacija.ucitajDatoteku(nazivDatoteke);
    if (this.ispis == 1) {
      for (String idLokacije : this.lokacije.keySet()) {
        var lokacija = this.lokacije.get(idLokacije);
        Logger.getGlobal().log(Level.INFO, "Lokacija: " + lokacija.naziv());
      }
    }
  }

  /**
   * Učitava sve uredaje iz CSV datoteke koja je definirana u postavci datotekaUredaja
   * 
   * @throws IOException - baca iznimku ako je problem s učitavanjem
   */
  public void ucitajUredaje() throws IOException {
    var nazivDatoteke = this.konf.dajPostavku("datotekaUredaja");
    var citacUredaja = new CitanjeUredaja();
    this.uredaji = citacUredaja.ucitajDatoteku(nazivDatoteke);
    if (this.ispis == 1) {
      for (String idUredaja : this.uredaji.keySet()) {
        var uredaj = this.uredaji.get(idUredaja);
        Logger.getGlobal().log(Level.INFO, "Uređaj: " + uredaj.naziv());
      }
    }
  }

  /**
   * Metoda koja otvara mrežna vrata na temelju postavki koje su spremljene u memoriji
   * 
   * @throws IOException - baca iznimku ako dođe do problema
   */
  public synchronized void otvoriMreznaVrata() {
    try {
      if (this.brojAktivnihDretvi < this.brojRadnika) {
        this.posluzitelj = new ServerSocket(this.mreznaVrata, this.brojCekaca);
        while (!this.kraj) {
          var uticnica = this.posluzitelj.accept();
          var dretva = new MrezniRadnik(uticnica, konf, this);
          dretva.setName("bgolubic_" + this.brojAktivnihDretvi);
          dretva.start();
          this.brojAktivnihDretvi++;
        }
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

  /**
   * Metoda koja gasi poslužitelj
   * 
   * @throws IOException - baca iznimku ako dođe do problema
   */
  public void ugasiPosluzitelj() {
    if (this.brojAktivnihDretvi == 0)
      try {
        this.posluzitelj.close();
      } catch (IOException e) {
        Logger.getGlobal().log(Level.INFO, "Poslužitelj ugašen");
      }
  }

  /**
   * Metoda koja otvara vrata prema poslužitelju udaljenosti
   * 
   * @throws IOException - baca iznimku ako dođe do problema
   */
  public boolean otvoriMreznaVrataPosluziteljaUdaljenosti() {
    try {
      var mreznaUticnica =
          new Socket(this.posluziteljUdaljenostiAdresa, this.posluziteljUdaljenostiVrata);
      mreznaUticnica.setSoTimeout(this.maksCekanje);
      this.mreznaUticnicaUdaljenosti = mreznaUticnica;
      return true;
    } catch (IOException e) {
      Logger.getGlobal().log(Level.SEVERE, e.getMessage());
    }
    return false;
  }
}
