package org.bsc.maven.plugin.confluence;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.generator.Generator;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.swizzle.confluence.Confluence;
import org.codehaus.swizzle.confluence.Page;
import org.jfrog.maven.annomojo.annotations.MojoComponent;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;

/**
 * Generate Plugin's documentation in confluence's wiki format
 * 
 * 
 *
 */
@MojoGoal("plugin-doc")
public class ConfluenceGeneratePluginDocMojo extends AbstractMavenReport {

    /**
     * Report output directory.
     *
     */
	@MojoParameter(expression="${project.build.directory}/generated-site/confluence",required=true)
    private String outputDirectory;

    /**
     * Doxia Site Renderer.
     *
     */
	@MojoComponent
    private SiteRenderer siteRenderer;

    /**
     * The Maven Project.
     *
     */
	@MojoParameter( expression="${project}", readonly=true, required=true)
    private MavenProject project;

    /**
     * Mojo scanner tools.
     *
     */
	@MojoComponent
    protected MojoScanner mojoScanner;

	/**
	 * Confluence end point url 
	 */
	@MojoParameter(expression="${confluence.endPoint}", defaultValue="http://localhost:8080/rpc/xmlrpc")
	private String endPoint;

	/**
	 * Confluence target confluence's spaceKey 
	 */
	@MojoParameter(expression="${confluence.spaceKey}", required=true)
	private String spaceKey;

	/**
	 * Confluence target confluence's spaceKey 
	 */
	@MojoParameter(expression="${confluence.parentPage}",defaultValue="Home")
	private String parentPageTitle;
	
	/**
	 * Confluence username 
	 */
	@MojoParameter(expression="${confluence.userName}",defaultValue="admin")
	private String username;

	/**
	 * Confluence password 
	 */
	@MojoParameter(expression="${confluence.password}")
	private String password;
	
    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }    

    @Override
	protected SiteRenderer getSiteRenderer() {
		return siteRenderer;
	}


    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    protected void executeReport( Locale locale )  throws MavenReportException
    {
        if ( !project.getPackaging().equals( "maven-plugin" ) )
        {
            return;
        }

        String goalPrefix = PluginDescriptor.getGoalPrefixFromArtifactId( project.getArtifactId() );

        // TODO: could use this more, eg in the writing of the plugin descriptor!
        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        pluginDescriptor.setGroupId( project.getGroupId() );

        pluginDescriptor.setArtifactId( project.getArtifactId() );

        pluginDescriptor.setVersion( project.getVersion() );

        pluginDescriptor.setGoalPrefix( goalPrefix );

        try
        {
            pluginDescriptor.setDependencies( PluginUtils.toComponentDependencies( project.getRuntimeDependencies() ) );

            mojoScanner.populatePluginDescriptor( project, pluginDescriptor );
            
            pluginDescriptor.setDescription( project.getDescription() );

            // Generate the plugin's documentation
            generatePluginDocumentation( pluginDescriptor );

            // Write the overview
            //PluginOverviewRenderer r = new PluginOverviewRenderer( getSink(), pluginDescriptor, locale );
            //r.render();
        }
        catch ( InvalidPluginDescriptorException e )
        {
            throw new MavenReportException( 
            		"Error extracting plugin descriptor: \'" + e.getLocalizedMessage() + "\'", e );
        }
        catch ( ExtractionException e )
        {
            throw new MavenReportException( "Error extracting plugin descriptor: \'" + e.getLocalizedMessage() + "\'",
                                            e );
        }
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.description" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "plugin-info";
    }

    private void generatePluginDocumentation( PluginDescriptor pluginDescriptor )
        throws MavenReportException
    {
        try
        {
    		
    		Confluence confluence = new Confluence( endPoint );
            confluence.login(username, password);

            File outputDir = new File( getOutputDirectory() );
            outputDir.mkdirs();

            getLog().info( "speceKey=" + spaceKey + " parentPageTitle=" + parentPageTitle);
            
            Page p = confluence.getPage(spaceKey, parentPageTitle);
            
            Generator generator = new PluginConfluenceDocGenerator(confluence, p); /*PluginXdocGenerator()*/;
            generator.execute( outputDir, pluginDescriptor );
            
            confluence.logout();
        }
        catch ( Exception e )
        {
            throw new MavenReportException( "Error writing plugin documentation", e );
        }

    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "plugin-report", locale, ConfluenceGeneratePluginDocMojo.class.getClassLoader() );
    }

    /**
     * Generates an overview page with the list of goals
     * and a link to the goal's page.
     */
    /*
    static class PluginOverviewRenderer extends AbstractMavenReportRenderer
    {
        private final PluginDescriptor pluginDescriptor;

        private final Locale locale;

        public PluginOverviewRenderer( Sink sink, PluginDescriptor pluginDescriptor, Locale locale )
        {
            super( sink );

            this.pluginDescriptor = pluginDescriptor;

            this.locale = locale;
        }

        public String getTitle()
        {
            return getBundle( locale ).getString( "report.plugin.title" );
        }

        @SuppressWarnings("unchecked")
		public void renderBody()
        {
            startSection( getTitle() );

            paragraph( getBundle( locale ).getString( "report.plugin.goals.intro" ) );

            startTable();

            String goalColumnName = getBundle( locale ).getString( "report.plugin.goals.column.goal" );
            String descriptionColumnName = getBundle( locale ).getString( "report.plugin.goals.column.description" );

            tableHeader( new String[]{goalColumnName, descriptionColumnName} );

            List<MojoDescriptor> mojos = pluginDescriptor.getMojos();
        	
        	if( mojos!=null ) {

	            for ( MojoDescriptor mojo : mojos )
	            {
	                String goalName = mojo.getFullGoalName();
	                String goalDocumentationLink = "./" + mojo.getGoal() + "-mojo.html";
	                String description = mojo.getDescription();
	                if ( StringUtils.isEmpty( mojo.getDescription() ) )
	                {
	                    description = getBundle( locale ).getString( "report.plugin.goal.nodescription" );
	
	                }
	
	                sink.tableRow();
	                tableCell( createLinkPatternedText( goalName, goalDocumentationLink ) );
	                tableCell( description, true );
	                sink.tableRow_();
	            }
        	}
            endTable();

            endSection();
        }
    }
*/
}