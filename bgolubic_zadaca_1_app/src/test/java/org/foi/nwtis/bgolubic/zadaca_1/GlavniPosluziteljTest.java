package org.foi.nwtis.bgolubic.zadaca_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import org.foi.nwtis.Konfiguracija;
import org.foi.nwtis.KonfiguracijaApstraktna;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class GlavniPosluziteljTest {

  private GlavniPosluzitelj gp;
  static Konfiguracija konf;

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
    konf = KonfiguracijaApstraktna.preuzmiKonfiguraciju("NWTiS_bgolubic_1.txt");
  }

  @AfterAll
  static void tearDownAfterClass() throws Exception {}

  @BeforeEach
  void setUp() throws Exception {
    this.gp = new GlavniPosluzitelj("NWTiS_bgolubic_1.txt");
  }

  @AfterEach
  void tearDown() throws Exception {
    this.gp = null;
  }

  @Disabled("Još treba implementirati!")
  @Test
  void testPokreniPosluzitelja() {
    fail("Not yet implemented");
  }

  @Disabled("Zasad ugašeno")
  @Test
  void akoImamoIspravnuDatotekuKadaPokrenemoUcitajKorisnikeTadaImamoKorisnike() {
    try {
      gp.ucitajKorisnike();
      assertEquals(10, gp.korisnici.size());
      assertEquals("Pero", gp.korisnici.get("pkos").ime());
      assertEquals("Kos", gp.korisnici.get("pkos").prezime());
      assertEquals(true, gp.korisnici.get("pkos").administrator());
      assertNull(gp.korisnici.get("bgolubic"));
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  @Disabled("Još treba implementirati!")
  @Test
  void testOtvoriMreznaVrata() {
    fail("Not yet implemented");
  }

}
