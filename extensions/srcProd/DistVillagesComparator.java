/*
  Copyright 2011 by Francesco Pizzitutti
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package extensions;

import java.util.*;

public class DistVillagesComparator implements Comparator, java.io.Serializable
{
   private static final long serialVersionUID = 1L;

   //----------------------------------------------------------
   public DistVillagesComparator()
   {
   }

   //----------------------------------------------------------
   public int compare(Object ao1, Object ao2)
   {
      HashMap <Long, Double> a1 = ((HashMap<Long, Double>)ao1);
      HashMap <Long, Double> a2 = ((HashMap<Long, Double>)ao2);

      double dist1 = 0.0;
      double dist2 = 0.0;

      for(Long j : a1.keySet())
      {
          dist1 = a1.get(j);
          break;
      }

      for(Long j : a2.keySet())
      {
          dist2 = a2.get(j);
          break;
      }



      if(dist1 > dist2)
          return 1;
      else if(dist1 < dist2)
          return -1;
      else
          return 0;
   }


}
