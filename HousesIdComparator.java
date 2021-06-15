/*
  Copyright 2011 by Francesco Pizzitutti
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.cystiagents;

import java.util.*;

public class HousesIdComparator implements Comparator, java.io.Serializable
{
   private static final long serialVersionUID = 1L;

   //----------------------------------------------------------
   public HousesIdComparator()
   {
   }

   //----------------------------------------------------------
   public int compare(Object ao1, Object ao2)
   {

      Household a1 = ((Household)ao1);
      Household a2 = ((Household)ao2);
 
      if(a1.simId > a2.simId)
          return 1;
      else if(a1.simId < a2.simId)
          return -1;
      else
          return 0;
   }


}
