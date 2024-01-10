package org.foi.nwtis.bgolubic.zadaca_1;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PokretacPosluzitelja {

  public PokretacPosluzitelja() {}

  public static void main(String[] args) {
    var pokretac = new PokretacPosluzitelja();
    if (!pokretac.provjeriArgumente(args)) {
      Logger.getLogger(PokretacPosluzitelja.class.getName()).log(Level.SEVERE,
          "Nije upisan naziv datoteke ili je dano previše argumenata.");
      return;
    }
    var glavniPosluzitelj = new GlavniPosluzitelj(args[0]);
    glavniPosluzitelj.pokreniPosluzitelj();

  }

  boolean provjeriArgumente(String[] args) {
    return args.length == 1 ? true : false;
  }
}
