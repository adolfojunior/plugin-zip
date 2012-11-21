package org.jboss.forge.zip.plugin;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.project.services.ResourceFactory;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.forge.zip.plugin.LsZipPlugin;
import org.jboss.forge.zip.resources.ZipEntryResource;
import org.jboss.forge.zip.resources.ZipResource;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mvel2.ConversionHandler;

public class LsZipPluginTest extends AbstractShellTest
{
   private static final String README_TXT = "README.TXT";

   private static final String README_CONTENT = "LsZipPluginTest Test File Content";

   private static final String PKG = LsZipPluginTest.class.getSimpleName().toLowerCase();

   private static File JAR;

   @Inject
   private ResourceFactory factory;

   private int outputPosition;

   @Deployment
   public static JavaArchive getDeployment()
   {
      return AbstractShellTest.getDeployment().addPackages(true, LsZipPlugin.class.getPackage());
   }

   @BeforeClass
   public static void createJar() throws IOException
   {
      JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test.jar")
               .addClass(LsZipPlugin.class)
               .addClass(ZipResource.class)
               .addClass(ZipEntryResource.class)
               .add(new ByteArrayAsset(README_CONTENT.getBytes()), ArchivePaths.create(README_TXT))
               .addAsManifestResource(new ByteArrayAsset("<beans/>".getBytes()), ArchivePaths.create("beans.xml"));

      JAR = File.createTempFile(PKG, ".jar");

      jar.as(ZipExporter.class).exportTo(JAR, true);
   }

   @AfterClass
   public static void deleteJar()
   {
      JAR.delete();
   }

   /**
    * Cant use annotation {@link Before}, for some reason the {@link ConversionHandler} is not registred.
    */
   // @Before
   public void prepare() throws Exception
   {
      Shell shell = getShell();

      shell.execute("cd " + JAR.getAbsolutePath());

      Resource<?> jarResource = factory.getResourceFrom(JAR);
      Resource<?> currentResource = shell.getCurrentResource();

      Assert.assertEquals("Command CD dont target the JAR.", jarResource.getFullyQualifiedName(),
               currentResource.getFullyQualifiedName());

      markOutput();
   }

   /**
    * Test the result of command "ls *.class" with wildcard. The current resource must be a JAR.
    */
   @Test
   public void testLs() throws Exception
   {
      prepare();

      Shell shell = getShell();

      shell.execute("ls");

      assertOutput(
               "META-INF/",
               "README.TXT",
               "org/");
   }

   /**
    * Test the result of command "ls *.class" with wildcard. The current resource must be a JAR.
    */
   @Test
   public void testLsWithPattern() throws Exception
   {
      prepare();

      Shell shell = getShell();

      shell.execute("ls *.TXT");

      assertOutput("README.TXT");
   }

   /**
    * Test the result of command "ls -a" with wildcard. The current resource must be a JAR.
    */
   @Test
   public void testLsAll() throws Exception
   {
      prepare();

      Shell shell = getShell();

      shell.execute("ls -a");

      assertOutput(
               "META-INF/",
               "META-INF/beans.xml",
               "README.TXT",
               "org/",
               "org/jboss/",
               "org/jboss/forge/",
               "org/jboss/forge/zip/",
			   "org/jboss/forge/zip/plugin",
               "org/jboss/forge/zip/plugin/LsZipPlugin.class",
               "org/jboss/forge/zip/plugin/LsZipPlugin$NameRegExFilter.class",
               "org/jboss/forge/zip/resources/",
               "org/jboss/forge/zip/resources/ZipEntryResource$FullyQualifiedNameComparator.class",
               "org/jboss/forge/zip/resources/ZipEntryResource$ZipEntryInputStream.class",
               "org/jboss/forge/zip/resources/ZipEntryResource.class",
               "org/jboss/forge/zip/resources/ZipResource.class");
   }

   /**
    * Test the result of command "ls -a *.class" with wildcard. The current resource must be a JAR.
    */
   @Test
   public void testLsAllWithPattern() throws Exception
   {
      prepare();

      Shell shell = getShell();

      shell.execute("ls -a *.class");

      assertOutput(
               "org/jboss/forge/zip/plugin/LsZipPlugin.class",
               "org/jboss/forge/zip/plugin/LsZipPlugin$NameRegExFilter.class",
               "org/jboss/forge/zip/resources/ZipEntryResource$FullyQualifiedNameComparator.class",
               "org/jboss/forge/zip/resources/ZipEntryResource$ZipEntryInputStream.class",
               "org/jboss/forge/zip/resources/ZipEntryResource.class",
               "org/jboss/forge/zip/resources/ZipResource.class");
   }

   /**
    * Test the result of command "ls -a" with wildcard. The current resource must be a JAR.
    */
   @Test
   public void testChangeDirectory() throws Exception
   {
      prepare();

      Shell shell = getShell();

      changeDirectory(shell, "org");
      changeDirectory(shell, "jboss");
      changeDirectory(shell, "forge");
      changeDirectory(shell, "zip");
      changeDirectory(shell, "plugin");

      markOutput();

      shell.execute("ls");

      assertOutput(
               "LsZipPlugin.class",
               "LsZipPlugin$NameRegExFilter.class");
      
      shell.execute("cd ../../../../..");
      
      markOutput();
      
      shell.execute("ls *.TXT");
      
      assertOutput("README.TXT");
   }

   /**
    * Test the result of command "ls". The current resource must be a JAR.
    */
   /**
    * @throws Exception
    */
   @Test
   public void testCat() throws Exception
   {
      prepare();

      Shell shell = getShell();

      String content = catCurrentResource(shell, README_TXT);

      Assert.assertEquals("Error reading content of resource " + README_TXT, README_CONTENT.trim(), content.trim());
   }

   private String catCurrentResource(Shell shell, String name) throws Exception
   {
      markOutput();

      shell.execute("cat " + name);

      return getMarkedOutput();
   }

   protected void changeDirectory(Shell shell, String name) throws Exception
   {
      shell.execute("cd " + name);

      Assert.assertEquals("Change Directory dont work for " + name, name, getShell().getCurrentResource().getName());
   }

   protected void assertOutput(String... lines) throws IOException
   {
      List<String> linesList = Arrays.asList(lines);
      List<String> outputList = IOUtils.readLines(new StringReader(getMarkedOutput()));

      for (String line : linesList)
      {
         Assert.assertTrue("Entry " + line + " is not in the output.", outputList.contains(line));
      }

      for (String line : outputList)
      {
         Assert.assertTrue("Output line " + line + " is not in archive.", linesList.contains(line));
      }
   }

   protected void markOutput()
   {
      // Save the index of output, for last by command.
      outputPosition = getOutput().length();
   }

   protected String getMarkedOutput()
   {
      return getOutput().substring(outputPosition);
   }
}
