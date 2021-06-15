/*
  Copyright 2011 by Francesco Pizzitutti
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package extensions;

import java.util.*;

public class SobolIndexRunsComparator implements Comparator, java.io.Serializable
{
   private static final long serialVersionUID = 1L;

   //----------------------------------------------------------
   public SobolIndexRunsComparator()
   {
   }

   //----------------------------------------------------------
   public int compare(Object ao1, Object ao2)
   {

      ABCRun a1 = ((ABCRun)ao1);
      ABCRun a2 = ((ABCRun)ao2);

      //System.out.println ("a1 sobolIndex: " + a1.sobolIndex + " a2 sobolIndex: " + a2.sobolIndex);

      //System.out.println ("a1 j: " + a1.num);
      //System.out.println ("a2 j: " + a2.num);

      //if(Double.isNaN(a1.sobolIndex))
      //{
      //    System.out.println ("run 1 sobolIndex: " + a1.sobolIndex);
      //    return 0;
      //}
      //if(Double.isNaN(a2.sobolIndex))
      //{
      //    System.out.println ("run 2 sobolIndex: " + a2.sobolIndex);
      //    return 0;
      //}
 
 
      if(a1.sobolIndex > a2.sobolIndex)
          return 1;
      else if(a1.sobolIndex < a2.sobolIndex)
          return -1;
      else
          return 0;
   }


}
