/*
 * Copyright (c) 2008, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of MSPSim.
 *
 * -----------------------------------------------------------------
 *
 * Main
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : 6 nov 2008
 */

package se.sics.mspsim;
import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.util.ArgumentManager;
import edu.clemson.eval.EvalLogger;
import se.sics.mspsim.util.QuickSim;

/**
 *
 */
public class Main {
	
  private static QuickSim quicksim = QuickSim.getInstance();

  public static GenericNode createNode(String className) {
    try {
      Class<? extends GenericNode> nodeClass = Class.forName(className).asSubclass(GenericNode.class);
      return nodeClass.newInstance();
    } catch (ClassNotFoundException e) {
      // Can not find specified class
    } catch (ClassCastException e) {
      // Wrong class type
    } catch (InstantiationException e) {
      // Failed to instantiate
    } catch (IllegalAccessException e) {
      // Failed to access constructor
    }
    return null;
  }

  public static String getNodeTypeByPlatform(String platform) {
      if ("jcreate".equals(platform)) {
          return "se.sics.mspsim.platform.jcreate.JCreateNode";
      }
      if ("sentilla-usb".equals(platform)) {
          return "se.sics.mspsim.platform.sentillausb.SentillaUSBNode";
      }
      if ("esb".equals(platform)) {
          return "se.sics.mspsim.platform.esb.ESBNode";
      }
      if ("exp5438".equals(platform)) {
          return "se.sics.mspsim.platform.ti.Exp5438Node";
      }
      if ("moo".equals(platform)) {
          return "se.sics.mspsim.platform.crfid.MooNode";
      }
      if ("wisp".equals(platform)) {
          return "se.sics.mspsim.platform.crfid.WispNode";
      }
      if ("exp6989".equalsIgnoreCase(platform)) {
    	  return "se.sics.mspsim.platform.ti.Exp6989Node";
      }
      if ("mayfly".equalsIgnoreCase(platform)) {
    	  return "edu.clemson.platform.MayflyNode";
      }
      if ("senseandsend".equalsIgnoreCase(platform)) {
    	  return "edu.clemson.platform.SenseAndSendNode";
      }
      if ("senseandsendsemi".equalsIgnoreCase(platform)) {
    	  return "edu.clemson.platform.SenseAndSendNodeSemi";
      }
      if ("senseandsendadap".equalsIgnoreCase(platform)) {
    	  return "edu.clemson.platform.SenseAndSendNodeAdap";
      }
      // Try to guess the node type.
      return "se.sics.mspsim.platform." + platform + '.'
          + Character.toUpperCase(platform.charAt(0))
          + platform.substring(1).toLowerCase() + "Node";
  }
  
  
  private static void addTracesToQuickSim (final File folder) {
	  for (final File fileEntry : folder.listFiles()) {
		  if (fileEntry.isDirectory()) {
			  addTracesToQuickSim(fileEntry);
		  } else {
			  quicksim.addTrace(folder + "/" + fileEntry.getName());
		  }
	  }
  }

  public static void main(String[] args) throws IOException {
    ArgumentManager config = new ArgumentManager();
    config.handleArguments(args);
    
    String evalsubdir = config.getProperty("evalsubdir");
    if (null != evalsubdir) {
    	EvalLogger.setSubdir(evalsubdir);
    }
    
    String ekhoTraceDir = config.getProperty("ekhotracedir");
    if (null != ekhoTraceDir) {
    	final File folder = new File(ekhoTraceDir);
    	addTracesToQuickSim(folder);
    	
    	ExecutorService es = Executors.newFixedThreadPool(quicksim.getTraces().size());

        for (String ekhotrace : quicksim.getTraces()) {
        	config.setProperty("ekhotrace", ekhotrace);
        	GenericNode node = runMain(config, args);
        	es.execute(node);
        }
        
        es.shutdown();

    } else {
    	runMain (config, args); // Just a single run
    }
  }
  
  private static GenericNode runMain (ArgumentManager config, String [] args) {
	  	String nodeType = config.getProperty("nodeType");
	    String platform = nodeType;
	    GenericNode node;
	    if (nodeType != null) {
	      node = createNode(nodeType);
	    } else {
	      platform = config.getProperty("platform");
	      if (platform == null) {
	          // Default platform
	          platform = "sky";
	
	          // Guess platform based on firmware filename suffix.
	          // TinyOS firmware files are often named 'main.exe'.
	          String[] a = config.getArguments();
	          if (a.length > 0 && !"main.exe".equals(a[0])) {
	              int index = a[0].lastIndexOf('.');
	              if (index > 0) {
	                  platform = a[0].substring(index + 1);
	              }
	          }
	      }
	      nodeType = getNodeTypeByPlatform(platform);
	      node = createNode(nodeType);
	    }
	    if (node == null) {
	      System.err.println("MSPSim does not currently support the platform '" + platform + "'.");
	      System.exit(1);
	    }
	   	try {
			node.setupArgs(config);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   	return node;
  }
}
