/*
  Copyright 2011 by Francesco Pizzitutti
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package extensions;

import java.util.*;

public class PopVillagesComparator implements Comparator, java.io.Serializable
{
   private static final long serialVersionUID = 1L;

   //----------------------------------------------------------
   public PopVillagesComparator()
   {
   }

   //----------------------------------------------------------
   public int compare(Object ao1, Object ao2)
   {

      Village a1 = ((Village)ao1);
      Village a2 = ((Village)ao2);
 
      if(a1.avgTotPop > a2.avgTotPop)
          return 1;
      else if(a1.avgTotPop < a2.avgTotPop)
          return -1;
      else
          return 0;
   }


}
