/*
  Copyright 2011 by Francesco Pizzitutti
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package extensions;

import java.util.*;

public class DistListListComparator implements Comparator, java.io.Serializable
{
   private static final long serialVersionUID = 1L;

   //----------------------------------------------------------
   public DistListListComparator()
   {
   }

   //----------------------------------------------------------
   public int compare(Object ao1, Object ao2)
   {

      List<Object> a1 = (List<Object>)ao1;
      List<Object> a2 = (List<Object>)ao2;

      //System.out.println ("a1 dist: " + a1.dist);
      //System.out.println ("a2 dist: " + a2.dist);

      //System.out.println ("a1 j: " + a1.num);
      //System.out.println ("a2 j: " + a2.num);

      //if(Double.isNaN(a1.dist))
      //{
      //    System.out.println ("run 1 dist: " + a1.dist);
      //    return 0;
      //}
      //if(Double.isNaN(a2.dist))
      //{
      //    System.out.println ("run 2 dist: " + a2.dist);
      //    return 0;
      //}
 
 
      if((double)a1.get(0) > (double)a2.get(0))
          return 1;
      else if((double)a1.get(0) < (double)a2.get(0))
          return -1;
      else
          return 0;
   }


}
