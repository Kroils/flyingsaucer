/*
 * {{{ header & license
 * Copyright (c) 2004 Joshua Marinacci
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.css;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import org.xhtmlrenderer.layout.Context;
import org.xhtmlrenderer.util.u;

/**
 * Description of the Class
 *
 * @author   empty
 */
public class FontResolverTest extends FontResolver {
    /** Description of the Field */
    String[] available_fonts;
    /** Description of the Field */
    HashMap instance_hash;
    /** Description of the Field */
    HashMap available_fonts_hash;

    /** Constructor for the FontResolverTest object */
    public FontResolverTest() {
        GraphicsEnvironment gfx = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available_fonts = gfx.getAvailableFontFamilyNames();
        //u.p("available fonts =");
        //u.p(available_fonts);
        instance_hash = new HashMap();

        // preload the font map with the font names as keys
        // don't add the actual font objects because that would be a waste of memory
        // we will only add them once we need to use them
        // put empty strings in instead
        available_fonts_hash = new HashMap();
        for ( int i = 0; i < available_fonts.length; i++ ) {
            available_fonts_hash.put( available_fonts[i], new String() );
        }

        // preload sans, serif, and monospace into the available font hash
        available_fonts_hash.put( "Serif", new Font( "Serif", Font.PLAIN, 1 ) );
        available_fonts_hash.put( "SansSerif", new Font( "SansSerif", Font.PLAIN, 1 ) );
        //u.p("put in sans serif");
        available_fonts_hash.put( "Monospaced", new Font( "Monospaced", Font.PLAIN, 1 ) );
    }
    public void setFontMapping(String name, Font font) {
        available_fonts_hash.put(name, font.deriveFont(1f));
    }

    /**
     * Description of the Method
     *
     * @param c         PARAM
     * @param families  PARAM
     * @param size      PARAM
     * @param weight    PARAM
     * @param style     PARAM
     * @return          Returns
     */
    public Font resolveFont( Context c, String[] families, float size, String weight, String style, String variant ) {
        //u.p("familes = ");
        //u.p(families);
        // for each font family
        if ( families != null ) {
            for ( int i = 0; i < families.length; i++ ) {
                Font font = resolveFont( c, families[i], size, weight, style, variant );
                if ( font != null ) {
                    return font;
                }
            }
        }

        // if we get here then no font worked, so just return default sans
        //u.p("pulling out: -" + available_fonts_hash.get("SansSerif") + "-");
        try {
            Font fnt = createFont( (Font)available_fonts_hash.get( "SansSerif" ), size, weight, style, variant );
            instance_hash.put( getFontInstanceHashName( "SansSerif", size, weight, style, variant ), fnt );
            //u.p("subbing in base sans : " + fnt);
            return fnt;
        } catch ( Exception ex ) {
            u.p( "exception: " + ex );
            return c.getGraphics().getFont();
        }

    }

    /**
     * Description of the Method
     *
     * @param root_font  PARAM
     * @param size       PARAM
     * @param weight     PARAM
     * @param style      PARAM
     * @return           Returns
     */
    protected Font createFont( Font root_font, float size, String weight, String style, String variant ) {
        int font_const = Font.PLAIN;
        if ( weight != null && weight.equals( "bold" ) ) {
            font_const = font_const | Font.BOLD;
        }
        if ( style != null && style.equals( "italic" ) ) {
            font_const = font_const | Font.ITALIC;
        }

        Font fnt = root_font.deriveFont( font_const, size );
        if ( variant != null) {
            if( variant.equals("small-caps")) {
                fnt = fnt.deriveFont((float)( ((float) fnt.getSize())*0.6));
            }
        }

        return fnt;
    }

    /**
     * Description of the Method
     *
     * @param c       PARAM
     * @param font    PARAM
     * @param size    PARAM
     * @param weight  PARAM
     * @param style   PARAM
     * @return        Returns
     */
    protected Font resolveFont( Context c, String font, float size, String weight, String style, String variant ) {
        //u.p("here");
        // strip off the "s if they are there
        if ( font.startsWith( "\"" ) ) {
            font = font.substring( 1 );
        }
        if ( font.endsWith( "\"" ) ) {
            font = font.substring( 0, font.length() - 1 );
        }

        //u.p("final font = " + font);
        // normalize the font name
        if ( font.equals( "serif" ) ) {
            font = "Serif";
        }
        if ( font.equals( "sans-serif" ) ) {
            font = "SansSerif";
        }
        if ( font.equals( "monospace" ) ) {
            font = "Monospaced";
        }

        // assemble a font instance hash name
        String font_instance_name = getFontInstanceHashName( font, size, weight, style, variant );
        //u.p("looking for font: " + font_instance_name);
        // check if the font instance exists in the hash table
        if ( instance_hash.containsKey( font_instance_name ) ) {
            // if so then return it
            return (Font)instance_hash.get( font_instance_name );
        }

        //u.p("font lookup failed for: " + font_instance_name);
        //u.p("searching for : " + font + " " + size + " " + weight + " " + style + " " + variant);


        // if not then
        //  does the font exist
        if ( available_fonts_hash.containsKey( font ) ) {
            //u.p("found an available font for: " + font);
            Object value = available_fonts_hash.get( font );
            // have we actually allocated the root font object yet?
            Font root_font = null;
            if ( value instanceof Font ) {
                root_font = (Font)value;
            } else {
                root_font = new Font( font, Font.PLAIN, 1 );
                available_fonts_hash.put( font, root_font );
            }

            // now that we have a root font, we need to create the correct version of it
            Font fnt = createFont( root_font, size, weight, style, variant );

            // add the font to the hash so we don't have to do this again
            instance_hash.put( font_instance_name, fnt );
            return fnt;
        }

        // we didn't find any possible matching font, so just return null
        return null;
    }

    /**
     * Gets the fontInstanceHashName attribute of the FontResolverTest object
     *
     * @param name    PARAM
     * @param size    PARAM
     * @param weight  PARAM
     * @param style   PARAM
     * @return        The fontInstanceHashName value
     */
    protected String getFontInstanceHashName( String name, float size, String weight, String style, String variant ) {
        return name + "-" + size + "-" + weight + "-" + style + "-" + variant;
    }
}

/*
 * $Id$
 *
 * $Log$
 * Revision 1.5  2004/11/12 02:23:56  joshy
 * added new APIs for rendering context, xhtmlpanel, and graphics2drenderer.
 * initial support for font mapping additions
 *
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.4  2004/11/08 21:18:20  joshy
 * preliminary small-caps implementation
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.3  2004/10/23 13:03:45  pdoubleya
 * Re-formatted using JavaStyle tool.
 * Cleaned imports to resolve wildcards except for common packages (java.io, java.util, etc)
 * Added CVS log comments at bottom.
 *
 *
 */

