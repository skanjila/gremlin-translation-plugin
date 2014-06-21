package org.neo4j.server.plugin.gremlin;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import junit.framework.Assert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.ImpermanentGraphDatabase;
import org.neo4j.server.rest.repr.OutputFormat;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.formats.JsonFormat;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GremlinPluginTest
{

    private static ImpermanentGraphDatabase neo4j = null;
    private static GremlinPlugin plugin = null;
    private static OutputFormat json = null;
    private static JSONParser parser = new JSONParser();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        json = new OutputFormat( new JsonFormat(),
                new URI( "http://localhost/" ), null );
        neo4j = new ImpermanentGraphDatabase( "target/db" );
        plugin = new GremlinPlugin();
        Graph graph = new Neo4jGraph( neo4j );
        graph.removeVertex( graph.getVertex( 0 ) );
        Vertex marko = graph.addVertex( "1" );
        marko.setProperty( "name", "marko" );
        marko.setProperty( "age", 29 );

        Vertex vadas = graph.addVertex( "2" );
        vadas.setProperty( "name", "vadas" );
        vadas.setProperty( "age", 27 );

        Vertex lop = graph.addVertex( "3" );
        lop.setProperty( "name", "lop" );
        lop.setProperty( "lang", "java" );

        Vertex josh = graph.addVertex( "4" );
        josh.setProperty( "name", "josh" );
        josh.setProperty( "age", 32 );

        Vertex ripple = graph.addVertex( "5" );
        ripple.setProperty( "name", "ripple" );
        ripple.setProperty( "lang", "java" );

        Vertex peter = graph.addVertex( "6" );
        peter.setProperty( "name", "peter" );
        peter.setProperty( "age", 35 );

        graph.addEdge( "7", marko, vadas, "knows" ).setProperty( "weight", 0.5f );
        graph.addEdge( "8", marko, josh, "knows" ).setProperty( "weight", 1.0f );
        graph.addEdge( "9", marko, lop, "created" ).setProperty( "weight", 0.4f );

        graph.addEdge( "10", josh, ripple, "created" ).setProperty( "weight",
                1.0f );
        graph.addEdge( "11", josh, lop, "created" ).setProperty( "weight", 0.4f );

        graph.addEdge( "12", peter, lop, "created" ).setProperty( "weight",
                0.2f );
    }

    private String exampleURI = "https://github.com/tinkerpop/gremlin/raw/master/data/graph-example-1.xml";

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testExecuteScriptVertex() throws Exception
    {
        JSONObject object = (JSONObject) parser.parse( json.format( GremlinPluginTest.executeTestScript( "g.v(1)" ) ) );
        Assert.assertEquals( 29l,
                ( (JSONObject) object.get( "data" ) ).get( "age" ) );
        Assert.assertEquals( "marko",
                ( (JSONObject) object.get( "data" ) ).get( "name" ) );
    }

    @Test
    public void testExecuteScriptVertices() throws Exception
    {
        JSONArray array = (JSONArray) parser.parse( json.format( GremlinPluginTest.executeTestScript( "g.V" ) ) );
        List<String> ids = new ArrayList<String>( Arrays.asList( "1", "2", "3",
                "4", "5", "6" ) );
        Assert.assertEquals( array.size(), 6 );
        for ( Object object : array )
        {
            String self = (String) ( (JSONObject) object ).get( "self" );
            String id = self.substring( self.lastIndexOf( "/" ) + 1 );
            ids.remove( id );
            String name = (String) ( (JSONObject) ( (JSONObject) object ).get( "data" ) ).get( "name" );
            if ( id.equals( "1" ) )
            {
                Assert.assertEquals( name, "marko" );
            }
            else if ( id.equals( "2" ) )
            {
                Assert.assertEquals( name, "vadas" );
            }
            else if ( id.equals( "3" ) )
            {
                Assert.assertEquals( name, "lop" );
            }
            else if ( id.equals( "4" ) )
            {
                Assert.assertEquals( name, "josh" );
            }
            else if ( id.equals( "5" ) )
            {
                Assert.assertEquals( name, "ripple" );
            }
            else if ( id.equals( "6" ) )
            {
                Assert.assertEquals( name, "peter" );
            }
            else
            {
                Assert.assertTrue( false );
            }

        }
        Assert.assertEquals( ids.size(), 0 );
    }

    @Test
    public void testExecuteScriptEdges() throws Exception
    {
        JSONArray array = (JSONArray) parser.parse( json.format( GremlinPluginTest.executeTestScript( "g.E" ) ) );
        List<String> ids = new ArrayList<String>( Arrays.asList( "0", "1", "2",
                "3", "4", "5" ) );
        Assert.assertEquals( array.size(), 6 );
        for ( Object object : array )
        {
            String self = (String) ( (JSONObject) object ).get( "self" );
            String id = self.substring( self.lastIndexOf( "/" ) + 1 );
            ids.remove( id );
            Double weight = (Double) ( (JSONObject) ( (JSONObject) object ).get( "data" ) ).get( "weight" );
            Assert.assertNotNull( weight );
            Assert.assertTrue( weight > 0.1 );
        }
        Assert.assertEquals( ids.size(), 0 );
    }

    @Test
    public void testExecuteScriptGraph() throws Exception
    {
        String ret = (String) parser.parse( json.format( GremlinPluginTest.executeTestScript( "g" ) ) );
        Assert.assertEquals( ret, "ImpermanentGraphDatabase [target/db]" );
    }

    @Test
    public void testExecuteScriptLong() throws Exception
    {
        Assert.assertEquals(
                1l,
                parser.parse( json.format( GremlinPluginTest.executeTestScript( "1" ) ) ) );
    }

    @Test
    public void testExecuteScriptLongs()
    {
        Assert.assertEquals(
                "[ 1, 2, 5, 6, 8 ]",
                json.format( GremlinPluginTest.executeTestScript( "[1,2,5,6,8]" ) ) );
    }

    @Test
    public void testExecuteScriptNull()
    {
        Assert.assertEquals(
                "\"null\"",
                json.format( GremlinPluginTest.executeTestScript( "for(i in 1..2){g.v(0)}" ) ) );
    }

    @Test
    public void testMultilineScriptWithLinebreaks()
    {
        Assert.assertEquals( "2",
                json.format( GremlinPluginTest.executeTestScript( "1;\n2" ) ) );
    }

    @Test
    public void testScriptWithPaths()
    {
        Assert.assertTrue(
                json.format( GremlinPluginTest.executeTestScript( ""
                                                                  + "GraphMLReader.inputGraph(g, new URL(\""+exampleURI +"\").openStream());"
                                                                  + "g.v(1).outE.inV.name.paths" ) ).contains( "1-knows->2" ) );
    }

    @Test
    public void testMultiThread()
    {
        for ( int i = 0; i < 250; i++ )
        {
            final int x = i;
            new Thread()
            {
                public void run()
                {
                    Assert.assertEquals(
                            x + "",
                            json.format( GremlinPluginTest.executeTestScript( "x="
                                                                              + x
                                                                              + "; x" ) ) );
                }
            }.start();
        }
    }

    private static Representation executeTestScript( final String script )
    {
        Transaction tx = null;
        try
        {
            tx = neo4j.beginTx();
            return plugin.executeScript( neo4j, script );
        }
        finally
        {
            tx.success();
            tx.finish();
        }
    }

    @Test
    public void testExecuteScriptGetVerticesBySpecifiedName() throws Exception
    {
        JSONObject object = (JSONObject) parser.parse( json.format( GremlinPluginTest.executeTestScript( "g.V[[name:'marko']] >> 1" ) ) );
        Assert.assertEquals(
                ( (JSONObject) object.get( "data" ) ).get( "name" ), "marko" );
        Assert.assertEquals(
                ( (JSONObject) object.get( "data" ) ).get( "age" ), 29l );
        String self = (String) ( (JSONObject) object ).get( "self" );
        Assert.assertEquals( self.substring( self.lastIndexOf( "/" ) + 1 ), "1" );
    }

}
