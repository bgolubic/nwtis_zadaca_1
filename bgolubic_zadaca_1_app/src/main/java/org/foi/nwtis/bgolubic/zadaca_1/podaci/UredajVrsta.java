package org.foi.nwtis.bgolubic.zadaca_1.podaci;

public enum UredajVrsta {
  SenzorTemperatura(1), SenzorVlaga(2), SenzorTlak(3), SenzorSvjetlo(4), SenzorKontakt(
      5), SenzorTemperaturaVlaga(50), SenzorTemperaturaTlak(51), SenzorTemperaturaVlagaTlak(
          52), AktuatorVentilator(
              100), AktuatorGrijanje(101), AktuatorRasvjeta(102), AktuatorVrata(103);

  private int broj;

  private UredajVrsta(int broj) {
    this.broj = broj;
  }

  public static UredajVrsta odBroja(int broj) {
    for (UredajVrsta uv : UredajVrsta.values()) {
      if (uv.broj == broj) {
        return uv;
      }
    }

    throw new IllegalArgumentException("Ne postoji vrsta za vrijednost: " + broj + ".");
  }
}
