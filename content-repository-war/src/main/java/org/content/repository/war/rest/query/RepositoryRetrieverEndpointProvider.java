/*******************************************************************************
 * The MIT License
 * 
 * Copyright (c) 2011 David Waltermire
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package org.content.repository.war.rest.query;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.content.repository.config.RepositoryConfiguration;
import org.scapdev.content.core.ContentRepository;
import org.scapdev.content.core.query.QueryResult;
import org.scapdev.content.model.ExternalIdentifier;
import org.scapdev.content.model.Key;
import org.scapdev.content.model.MetadataModel;

@Path("/content/query/")
public class RepositoryRetrieverEndpointProvider {

//	private static Logger log = LoggerFactory.getLogger(RepositoryRetrieverEndpointProvider.class);
	
	@Path("/get/global/{id}")
	@GET
	@Produces("text/xml")
	public QueryResult getQualifiedId(@PathParam("id") String identifier, @QueryParam("resolve-relationships") boolean resolveRelationships ) throws IOException {
		// TODO: handle errors
		ContentRepository repository = RepositoryConfiguration.INSTANCE.getRepo();
		MetadataModel model = repository.getMetadataModel();

		QueryResult result = null;

		Key key = model.getKeyFromMappedIdentifier(identifier);
		if (key != null) {
			result = repository.query(key, resolveRelationships);
		} else {
			ExternalIdentifier externalIdentifier = model.getExternalIdentifierById(identifier);
			if (externalIdentifier != null) {
				result = repository.query(externalIdentifier, resolveRelationships);
			}
		}
		return result;
	}
}
