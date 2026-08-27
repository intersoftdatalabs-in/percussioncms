/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.rest.contenttypes;

import com.percussion.rest.ObjectLockSummary;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean(value = "restContentTypesResource")
@Path("/contenttypes")
@XmlRootElement
@Tag(name = "Content Types", description = "Content Type operations")
public class ContentTypesResource {

  private final IContentTypesAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public ContentTypesResource() {
    this.adaptor = null;
  }

  @Autowired
  public ContentTypesResource(IContentTypesAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  private IContentTypesAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "ContentTypes adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }

  /**
   * Map adaptor failures to HTTP status without sniffing message substrings (names such as {@code
   * percBlockquote} must not become 409).
   */
  private static WebApplicationException mapMutationFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof ContentTypeDesignLockException) {
      String msg = e.getMessage() != null ? e.getMessage() : "Conflict";
      return new WebApplicationException(msg, 409);
    }
    if (e instanceof IllegalArgumentException) {
      return new WebApplicationException(e.getMessage(), 400);
    }
    if (e instanceof IllegalStateException) {
      return new WebApplicationException(e, 500);
    }
    return new WebApplicationException(e, 500);
  }

  @GET
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List available ContentTypes",
      description = "Lists all available Content Types on the system.  Not filtered by security.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ContentType.class)),
                    examples =
                        @ExampleObject(
                            value =
                                "{\n"
                                    + "  \"ContentType\": [\n"
                                    + "    {\n"
                                    + "      \"description\": \"The Content Type for Page Template"
                                    + " items\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 303,\n"
                                    + "        \"stringValue\": \"0-2-303\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-303\",\n"
                                    + "        \"uuid\": 303\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percPageTemplate\",\n"
                                    + "      \"name\": \"percPageTemplate\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 301,\n"
                                    + "        \"stringValue\": \"0-2-301\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-301\",\n"
                                    + "        \"uuid\": 301\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"page\",\n"
                                    + "      \"name\": \"percPage\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 307,\n"
                                    + "        \"stringValue\": \"0-2-307\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-307\",\n"
                                    + "        \"uuid\": 307\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"HTML\",\n"
                                    + "      \"name\": \"percRawHtmlAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 305,\n"
                                    + "        \"stringValue\": \"0-2-305\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-305\",\n"
                                    + "        \"uuid\": 305\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percUserProfile\",\n"
                                    + "      \"name\": \"percUserProfile\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 311,\n"
                                    + "        \"stringValue\": \"0-2-311\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-311\",\n"
                                    + "        \"uuid\": 311\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Simple Text\",\n"
                                    + "      \"name\": \"percSimpleTextAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 309,\n"
                                    + "        \"stringValue\": \"0-2-309\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-309\",\n"
                                    + "        \"uuid\": 309\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Rich Text\",\n"
                                    + "      \"name\": \"percRichTextAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"Content Type for basic Managed"
                                    + " Navigation Content Items.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 314,\n"
                                    + "        \"stringValue\": \"0-2-314\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-314\",\n"
                                    + "        \"uuid\": 314\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Navon\",\n"
                                    + "      \"name\": \"rffNavon\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"Managed Navigation Content Item"
                                    + " used in the root Folder of a Site.  The Site root Folder"
                                    + " must contain a NavTree Content Item for Managed Navigation"
                                    + " to work.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 315,\n"
                                    + "        \"stringValue\": \"0-2-315\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-315\",\n"
                                    + "        \"uuid\": 315\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"NavTree\",\n"
                                    + "      \"name\": \"rffNavTree\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"A binary image file used in Managed"
                                    + " Navigation.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 313,\n"
                                    + "        \"stringValue\": \"0-2-313\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-313\",\n"
                                    + "        \"uuid\": 313\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Nav Image\",\n"
                                    + "      \"name\": \"rffNavImage\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 1726,\n"
                                    + "        \"stringValue\": \"0-2-1726\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-1726\",\n"
                                    + "        \"uuid\": 1726\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percSocialButtons\",\n"
                                    + "      \"name\": \"percSocialButtons\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"A binary image file used in Managed"
                                    + " Navigation.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 2239,\n"
                                    + "        \"stringValue\": \"0-2-2239\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-2239\",\n"
                                    + "        \"uuid\": 2239\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Nav Image\",\n"
                                    + "      \"name\": \"percNavImage\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"Stores all property data for all"
                                    + " widgets.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 2237,\n"
                                    + "        \"stringValue\": \"0-2-2237\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-2237\",\n"
                                    + "        \"uuid\": 2237\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"PSWidgetProperties\",\n"
                                    + "      \"name\": \"PSWidgetProperties\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 1730,\n"
                                    + "        \"stringValue\": \"0-2-1730\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-1730\",\n"
                                    + "        \"uuid\": 1730\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Open Graph\",\n"
                                    + "      \"name\": \"percOpenGraph\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 1728,\n"
                                    + "        \"stringValue\": \"0-2-1728\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-1728\",\n"
                                    + "        \"uuid\": 1728\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percImageSlider\",\n"
                                    + "      \"name\": \"percImageSlider\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 1223,\n"
                                    + "        \"stringValue\": \"0-2-1223\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-1223\",\n"
                                    + "        \"uuid\": 1223\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percDirectory\",\n"
                                    + "      \"name\": \"percDirectory\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"Content Type for basic Managed"
                                    + " Navigation Content Items.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 324,\n"
                                    + "        \"stringValue\": \"0-2-324\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-324\",\n"
                                    + "        \"uuid\": 324\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Navon\",\n"
                                    + "      \"name\": \"percNavon\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 1732,\n"
                                    + "        \"stringValue\": \"0-2-1732\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-1732\",\n"
                                    + "        \"uuid\": 1732\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Twitter Cards\",\n"
                                    + "      \"name\": \"percTwitterCards\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"Managed Navigation Content Item"
                                    + " used in the root Folder of a Site.  The Site root Folder"
                                    + " must contain a NavTree Content Item for Managed Navigation"
                                    + " to work.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 325,\n"
                                    + "        \"stringValue\": \"0-2-325\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-325\",\n"
                                    + "        \"uuid\": 325\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"NavTree\",\n"
                                    + "      \"name\": \"percNavTree\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 1221,\n"
                                    + "        \"stringValue\": \"0-2-1221\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-1221\",\n"
                                    + "        \"uuid\": 1221\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percDepartment\",\n"
                                    + "      \"name\": \"percDepartment\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 1227,\n"
                                    + "        \"stringValue\": \"0-2-1227\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-1227\",\n"
                                    + "        \"uuid\": 1227\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percPerson\",\n"
                                    + "      \"name\": \"percPerson\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"Manages binary files other than"
                                    + " images, such as .pdf or .doc.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 331,\n"
                                    + "        \"stringValue\": \"0-2-331\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-331\",\n"
                                    + "        \"uuid\": 331\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"File\",\n"
                                    + "      \"name\": \"percFileAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 1225,\n"
                                    + "        \"stringValue\": \"0-2-1225\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-1225\",\n"
                                    + "        \"uuid\": 1225\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percOrganization\",\n"
                                    + "      \"name\": \"percOrganization\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 329,\n"
                                    + "        \"stringValue\": \"0-2-329\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-329\",\n"
                                    + "        \"uuid\": 329\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Event\",\n"
                                    + "      \"name\": \"percEventAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"A widget that displays an 'accept"
                                    + " consent' dialog on the website.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 1356,\n"
                                    + "        \"stringValue\": \"0-2-1356\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-1356\",\n"
                                    + "        \"uuid\": 1356\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percCookieConsent\",\n"
                                    + "      \"name\": \"percCookieConsent\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 333,\n"
                                    + "        \"stringValue\": \"0-2-333\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-333\",\n"
                                    + "        \"uuid\": 333\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percFileAutoList\",\n"
                                    + "      \"name\": \"percFileAutoList\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 339,\n"
                                    + "        \"stringValue\": \"0-2-339\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-339\",\n"
                                    + "        \"uuid\": 339\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Comments Form\",\n"
                                    + "      \"name\": \"percCommentsFormAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 337,\n"
                                    + "        \"stringValue\": \"0-2-337\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-337\",\n"
                                    + "        \"uuid\": 337\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Form\",\n"
                                    + "      \"name\": \"percFormAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 343,\n"
                                    + "        \"stringValue\": \"0-2-343\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-343\",\n"
                                    + "        \"uuid\": 343\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percBlogIndexAsset\",\n"
                                    + "      \"name\": \"percBlogIndexAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 341,\n"
                                    + "        \"stringValue\": \"0-2-341\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-341\",\n"
                                    + "        \"uuid\": 341\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Page Auto List\",\n"
                                    + "      \"name\": \"percPageAutoList\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"Category widget\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 347,\n"
                                    + "        \"stringValue\": \"0-2-347\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-347\",\n"
                                    + "        \"uuid\": 347\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Categories\",\n"
                                    + "      \"name\": \"percCategoryList\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"Tag widget\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 345,\n"
                                    + "        \"stringValue\": \"0-2-345\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-345\",\n"
                                    + "        \"uuid\": 345\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Tags\",\n"
                                    + "      \"name\": \"percTagList\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 351,\n"
                                    + "        \"stringValue\": \"0-2-351\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-351\",\n"
                                    + "        \"uuid\": 351\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Archives\",\n"
                                    + "      \"name\": \"percArchiveList\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 861,\n"
                                    + "        \"stringValue\": \"0-2-861\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-861\",\n"
                                    + "        \"uuid\": 861\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"preRedirectWidget\",\n"
                                    + "      \"name\": \"preRedirectWidget\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 349,\n"
                                    + "        \"stringValue\": \"0-2-349\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-349\",\n"
                                    + "        \"uuid\": 349\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Calendar\",\n"
                                    + "      \"name\": \"percCalendarAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"Title Widget\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 355,\n"
                                    + "        \"stringValue\": \"0-2-355\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-355\",\n"
                                    + "        \"uuid\": 355\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percTitleAsset\",\n"
                                    + "      \"name\": \"percTitleAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 353,\n"
                                    + "        \"stringValue\": \"0-2-353\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-353\",\n"
                                    + "        \"uuid\": 353\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Image Auto List\",\n"
                                    + "      \"name\": \"percImageAutoList\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"A widget to read rss feeds.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 359,\n"
                                    + "        \"stringValue\": \"0-2-359\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-359\",\n"
                                    + "        \"uuid\": 359\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"RSS\",\n"
                                    + "      \"name\": \"percRssAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 357,\n"
                                    + "        \"stringValue\": \"0-2-357\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-357\",\n"
                                    + "        \"uuid\": 357\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Blog Post\",\n"
                                    + "      \"name\": \"percBlogPostAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"This is a system contenttype for"
                                    + " folders.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 101,\n"
                                    + "        \"stringValue\": \"0-2-101\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-101\",\n"
                                    + "        \"uuid\": 101\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Folder\",\n"
                                    + "      \"name\": \"Folder\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 363,\n"
                                    + "        \"stringValue\": \"0-2-363\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-363\",\n"
                                    + "        \"uuid\": 363\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Registration Asset (Deprecated)\",\n"
                                    + "      \"name\": \"percRegistrationAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"Login Asset for Content"
                                    + " configuration.\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 361,\n"
                                    + "        \"stringValue\": \"0-2-361\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-361\",\n"
                                    + "        \"uuid\": 361\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Login\",\n"
                                    + "      \"name\": \"percLoginAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 367,\n"
                                    + "        \"stringValue\": \"0-2-367\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-367\",\n"
                                    + "        \"uuid\": 367\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Secure Login (Deprecated)\",\n"
                                    + "      \"name\": \"percSecureLogin\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 365,\n"
                                    + "        \"stringValue\": \"0-2-365\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-365\",\n"
                                    + "        \"uuid\": 365\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Polls\",\n"
                                    + "      \"name\": \"percPollAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 1011,\n"
                                    + "        \"stringValue\": \"0-2-1011\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-1011\",\n"
                                    + "        \"uuid\": 1011\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percLocalLanguage\",\n"
                                    + "      \"name\": \"percLocalLanguage\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 369,\n"
                                    + "        \"stringValue\": \"0-2-369\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-369\",\n"
                                    + "        \"uuid\": 369\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"Image Asset\",\n"
                                    + "      \"name\": \"percImageAsset\"\n"
                                    + "    },\n"
                                    + "    {\n"
                                    + "      \"description\": \"\",\n"
                                    + "      \"guid\": {\n"
                                    + "        \"hostId\": 0,\n"
                                    + "        \"longValue\": 1009,\n"
                                    + "        \"stringValue\": \"0-2-1009\",\n"
                                    + "        \"type\": 2,\n"
                                    + "        \"untypedString\": \"0-1009\",\n"
                                    + "        \"uuid\": 1009\n"
                                    + "      },\n"
                                    + "      \"hideFromMenu\": false,\n"
                                    + "      \"label\": \"percDefaultLanguage\",\n"
                                    + "      \"name\": \"percDefaultLanguage\"\n"
                                    + "    }\n"
                                    + "  ]\n"
                                    + "}"))),
        @ApiResponse(responseCode = "404", description = "No Content Types found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ContentType> listContentTypes() {
    return new ContentTypeList(requireAdaptor().listContentTypes(uriInfo.getBaseUri()));
  }

  @GET
  @Path("/by-site/{id}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List available Content Types by Site",
      description = "Lists Content Types available for a site.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ContentType.class)))),
        @ApiResponse(responseCode = "404", description = "No Content Types found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ContentType> getContentTypesBySite(@PathParam("id") int siteId) {
    return new ContentTypeList(requireAdaptor().listContentTypes(uriInfo.getBaseUri(), siteId));
  }

  @GET
  @Consumes({MediaType.APPLICATION_JSON})
  @Path("/by-filter")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List available Content Types by Filter",
      description = "Lists Content Types available for a given filter.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ContentType.class)))),
        @ApiResponse(responseCode = "404", description = "No Content Types found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ContentType> listContentTypesByFilter(
      @RequestBody(
              description = "ContentTypeFilter to use for listing content types.",
              required = true,
              content = @Content(schema = @Schema(implementation = ContentTypeFilter.class)))
          @Valid
          ContentTypeFilter filter) {
    try {
      List<ContentType> results =
          requireAdaptor().listContentTypesByFilter(uriInfo.getBaseUri(), filter);
      if (results == null || results.isEmpty()) {
        throw new WebApplicationException("Not Found.", 404);
      }
      return results;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      // Preserve cause so log analysis retains the original stack/type
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{idOrName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get content type design summary",
      description =
          "Content type detail including field catalog. Field rows include control property"
              + " names and values. Choice catalogs and property write use"
              + " GET/PUT .../fields/{fieldName}/controlProperties. Field rule expressions use"
              + " GET/PUT .../fields/{fieldName}/ruleExpressions (see designGaps).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ContentTypeDetail.class))),
        @ApiResponse(responseCode = "404", description = "Content type not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ContentTypeDetail getContentType(@PathParam("idOrName") String idOrName) {
    try {
      ContentTypeDetail detail = requireAdaptor().getContentType(uriInfo.getBaseUri(), idOrName);
      if (detail == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return detail;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      // Preserve cause so log analysis retains the original stack/type
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update content type design fields",
      description =
          "Admin. Requires a design-session lock already held by the current user/session"
              + " (POST .../lock). Locks expire after 30 minutes; a PUT after expiry returns 409"
              + " and the client must re-lock. The save load extends a still-valid lock (it does"
              + " not release). Applies mutable fields (label, description, enabled, per-field"
              + " searchable/occurrence, allowedWorkflows + defaultWorkflow, allowedTemplates)"
              + " and saves without releasing the lock (POST .../unlock). Association lists:"
              + " omit/null = leave unchanged; non-null list = full replace (empty clears"
              + " workflows/templates). GET responses always include association arrays (may be"
              + " empty). Template associations are written after content-type save in a separate"
              + " design call — if that fails, meta/field/workflow changes may already be"
              + " committed (error message indicates partial success). Name/id and system field"
              + " structure are not changed. Field rule expressions use PUT"
              + " .../fields/{fieldName}/ruleExpressions. Control property values use PUT"
              + " .../fields/{fieldName}/controlProperties. Create/delete remain unsupported (see"
              + " designGaps).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated (lock is still held)",
            content = @Content(schema = @Schema(implementation = ContentTypeDetail.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Content type not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Design lock required, or locked by another user"),
        @ApiResponse(
            responseCode = "500",
            description =
                "Error — may indicate partial success when template association save fails after"
                    + " content type was already saved"),
      })
  public ContentTypeDetail updateContentType(
      @PathParam("idOrName") String idOrName, ContentTypeDetail body) {
    try {
      ContentTypeDetail detail =
          requireAdaptor().updateContentType(uriInfo.getBaseUri(), idOrName, body);
      if (detail == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return detail;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/{idOrName}/lock")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Lock content type design session",
      description =
          "Acquires a self-only design-session lock for the current Admin user via the content"
              + " design web service (IPSContentDesignWs.loadContentTypes with lock=true,"
              + " overrideLock=false). Does not save. Locks expire after 30 minutes"
              + " (PSObjectLock.LOCK_INTERVAL). Re-lock by the same session user extends the lock."
              + " remainingTime is the actual remaining minutes from the lock service.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Locked",
            content = @Content(schema = @Schema(implementation = ObjectLockSummary.class))),
        @ApiResponse(responseCode = "400", description = "Invalid id or wildcard name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Content type not found"),
        @ApiResponse(responseCode = "409", description = "Locked by another user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ObjectLockSummary lockContentType(@PathParam("idOrName") String idOrName) {
    try {
      ObjectLockSummary summary =
          requireAdaptor().lockContentType(uriInfo.getBaseUri(), idOrName);
      if (summary == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return summary;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/{idOrName}/unlock")
  @Operation(
      summary = "Unlock content type design session",
      description =
          "Releases a design-session lock owned by the current Admin user/session. Does not save."
              + " Locks held by another user are not stolen (409).",
      responses = {
        @ApiResponse(responseCode = "204", description = "Unlocked"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Content type not found"),
        @ApiResponse(responseCode = "409", description = "Locked by another user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response unlockContentType(@PathParam("idOrName") String idOrName) {
    try {
      Boolean released = requireAdaptor().unlockContentType(uriInfo.getBaseUri(), idOrName);
      if (released == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return Response.noContent().build();
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/enabled")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Enable or disable a content type",
      description =
          "CD-13 design action: sets the content type enabled flag for runtime use. Admin only."
              + " Requires a design-session lock already held by the current user (POST"
              + " .../lock). Does not acquire or release the lock. GET .../{idOrName} reflects"
              + " enabled after a successful PUT. Jackson root wrap is ContentTypeEnabled.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated (lock is still held)",
            content = @Content(schema = @Schema(implementation = ContentTypeDetail.class))),
        @ApiResponse(responseCode = "400", description = "enabled is required"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Content type not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Design lock required, or locked by another user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ContentTypeDetail setContentTypeEnabled(
      @PathParam("idOrName") String idOrName, ContentTypeEnabled body) {
    if (body == null || body.getEnabled() == null) {
      throw new WebApplicationException("enabled is required", 400);
    }
    try {
      ContentTypeDetail detail =
          requireAdaptor()
              .setContentTypeEnabled(uriInfo.getBaseUri(), idOrName, body.getEnabled());
      if (detail == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return detail;
    } catch (RuntimeException e) {
      throw mapEnabledFailure(e);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Maps enable/disable failures. Typed lock conflicts are 409; do not infer 409 from generic
   * IllegalStateException message substrings.
   */
  private static WebApplicationException mapEnabledFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof ContentTypeDesignLockException) {
      String msg = e.getMessage() != null ? e.getMessage() : "Conflict";
      return new WebApplicationException(msg, 409);
    }
    if (e instanceof IllegalArgumentException) {
      return new WebApplicationException(e.getMessage(), 400);
    }
    return new WebApplicationException(e, 500);
  }

  @PUT
  @Path("/{idOrName}/allowedWorkflows")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Replace content type allowed-workflow associations",
      description =
          "CD-08 design action: full-replace of allowedWorkflows (and optional defaultWorkflow)."
              + " Admin only. Requires a design-session lock already held by the current user"
              + " (POST .../lock). Does not acquire or release the lock. Empty allowedWorkflows"
              + " clears associations. Workflow ids are validated (name or guid of an existing"
              + " workflow). GET .../{idOrName} reflects the new set. Jackson root wrap is"
              + " ContentTypeWorkflows.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Updated (lock is still held)",
            content = @Content(schema = @Schema(implementation = ContentTypeDetail.class))),
        @ApiResponse(
            responseCode = "400",
            description = "allowedWorkflows is required, or a workflow id is invalid"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Content type not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Design lock required, or locked by another user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ContentTypeDetail setAllowedWorkflows(
      @PathParam("idOrName") String idOrName, ContentTypeWorkflows body) {
    if (body == null || body.getAllowedWorkflows() == null) {
      throw new WebApplicationException("allowedWorkflows is required", 400);
    }
    try {
      ContentTypeDetail detail =
          requireAdaptor()
              .setAllowedWorkflows(
                  uriInfo.getBaseUri(),
                  idOrName,
                  body.getAllowedWorkflows(),
                  body.getDefaultWorkflow());
      if (detail == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return detail;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      throw mapWriteFailure(e);
    }
  }

  @GET
  @Path("/{idOrName}/allowedTemplates")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List allowed templates for a content type",
      description =
          "Read-only list of templates associated with the content type (CD-12 GET). No design"
              + " lock is required. Empty list means none. Same association set as"
              + " ContentTypeDetail.allowedTemplates.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = NamedObjectRefList.class))),
        @ApiResponse(responseCode = "404", description = "Content type not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public NamedObjectRefList getAllowedTemplates(@PathParam("idOrName") String idOrName) {
    try {
      List<NamedObjectRef> items =
          requireAdaptor().getAllowedTemplates(uriInfo.getBaseUri(), idOrName);
      if (items == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return asNamedObjectRefList(items);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/allowedTemplates")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Replace allowed templates for a content type",
      description =
          "Full-replace of allowed template associations (CD-12). Requires a design-session lock"
              + " already held by the current user (POST .../lock when that slice is installed, or"
              + " an equivalent design lock). Does not acquire or release the lock. Empty list"
              + " clears associations. Template refs must include a resolvable name or guid."
              + " After PUT, GET on this path (or GET content type detail) lists the new set.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Replaced",
            content = @Content(schema = @Schema(implementation = NamedObjectRefList.class))),
        @ApiResponse(responseCode = "400", description = "Invalid template id/name or body"),
        @ApiResponse(responseCode = "404", description = "Content type not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Design lock not held by the current user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public NamedObjectRefList replaceAllowedTemplates(
      @PathParam("idOrName") String idOrName, List<NamedObjectRef> body) {
    try {
      List<NamedObjectRef> items =
          requireAdaptor().replaceAllowedTemplates(uriInfo.getBaseUri(), idOrName, body);
      if (items == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return asNamedObjectRefList(items);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw mapWriteFailure(e);
    }
  }

  @GET
  @Path("/{idOrName}/itemExits")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get item-level exits and validations for a content type",
      description =
          "CD-09 GET: item-level input/output translations, validations, and pipe pre/post"
              + " exits. No design lock is required. Empty lists mean none. Apply-when conditions"
              + " are summarized as read-only text (see designGaps). Jackson root wrap is"
              + " ContentTypeItemExits.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ContentTypeItemExits.class))),
        @ApiResponse(responseCode = "404", description = "Content type not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ContentTypeItemExits getItemExits(@PathParam("idOrName") String idOrName) {
    try {
      ContentTypeItemExits out = requireAdaptor().getItemExits(uriInfo.getBaseUri(), idOrName);
      if (out == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return out;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/itemExits")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Replace item-level exits and validations for a content type",
      description =
          "CD-09 PUT: full replace of item-level inputTranslations, outputTranslations, and"
              + " validations via IPSContentDesignWs.saveContentTypes. Requires a held"
              + " design-session lock (POST .../lock). Does not acquire or release the lock."
              + " Empty lists clear. preExits/postExits omitted leave pipe extensions unchanged;"
              + " empty list clears. Each exit needs a resolvable extension FQN. Apply-when"
              + " conditions are not written. Jackson root wrap is ContentTypeItemExits.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Replaced (lock is still held)",
            content = @Content(schema = @Schema(implementation = ContentTypeItemExits.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Missing required lists, invalid extension FQN, or design-save validation"
                    + " (wrong item-level extension interface / SAVE_FAILED validation)"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Content type not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Design lock not held by the current user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ContentTypeItemExits replaceItemExitsFromJson(
      @PathParam("idOrName") String idOrName, java.io.InputStream jsonBody) {
    try {
      byte[] raw = jsonBody == null ? new byte[0] : jsonBody.readAllBytes();
      String json = new String(raw, java.nio.charset.StandardCharsets.UTF_8);
      return replaceItemExits(idOrName, ContentTypeItemExitsJsonReader.parse(json));
    } catch (java.io.IOException e) {
      throw new WebApplicationException("Invalid item-exits body", 400);
    }
  }

  /**
   * Replace item-level exits. Package tests call this with a bound DTO; HTTP PUT uses {@link
   * #replaceItemExitsFromJson} so CXF UNWRAP_ROOT_VALUE cannot drop required lists (#3905).
   */
  public ContentTypeItemExits replaceItemExits(
      String idOrName, ContentTypeItemExits body) {
    if (body == null
        || body.getInputTranslations() == null
        || body.getOutputTranslations() == null
        || body.getValidations() == null) {
      throw new WebApplicationException(
          "inputTranslations, outputTranslations, and validations are required", 400);
    }
    try {
      ContentTypeItemExits out =
          requireAdaptor().replaceItemExits(uriInfo.getBaseUri(), idOrName, body);
      if (out == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return out;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      throw mapWriteFailure(e);
    }
  }

  @GET
  @Path("/{idOrName}/fields/{fieldName}/controlProperties")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get field control property values and choice catalog",
      description =
          "CD-07 GET: control parameter name/value pairs and the choice catalog for one field."
              + " No design lock is required. Empty properties means none. choices is omitted when"
              + " none. Jackson root wrap is ContentTypeFieldControlProperties.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    schema = @Schema(implementation = ContentTypeFieldControlProperties.class))),
        @ApiResponse(responseCode = "404", description = "Content type or field not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ContentTypeFieldControlProperties getFieldControlProperties(
      @PathParam("idOrName") String idOrName, @PathParam("fieldName") String fieldName) {
    try {
      ContentTypeFieldControlProperties out =
          requireAdaptor()
              .getFieldControlProperties(uriInfo.getBaseUri(), idOrName, fieldName);
      if (out == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return out;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/fields/{fieldName}/controlProperties")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Replace field control property values and optional choice catalog",
      description =
          "CD-07 PUT: full replace of control parameter values via IPSContentDesignWs"
              + " .saveContentTypes. Requires a held design-session lock (POST .../lock). Does not"
              + " acquire or release the lock. Empty properties clears parameters. choices omitted"
              + " leaves the catalog unchanged; type none clears. Jackson root wrap is"
              + " ContentTypeFieldControlProperties.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Replaced (lock is still held)",
            content =
                @Content(
                    schema = @Schema(implementation = ContentTypeFieldControlProperties.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing properties or invalid choice catalog"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Content type or field not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Design lock not held by the current user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ContentTypeFieldControlProperties replaceFieldControlProperties(
      @PathParam("idOrName") String idOrName,
      @PathParam("fieldName") String fieldName,
      ContentTypeFieldControlProperties body) {
    if (body == null || body.getProperties() == null) {
      throw new WebApplicationException("properties is required", 400);
    }
    try {
      ContentTypeFieldControlProperties out =
          requireAdaptor()
              .replaceFieldControlProperties(
                  uriInfo.getBaseUri(), idOrName, fieldName, body);
      if (out == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return out;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      throw mapWriteFailure(e);
    }
  }

  @GET
  @Path("/{idOrName}/fields/{fieldName}/ruleExpressions")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get field rule expressions",
      description =
          "CD-05–07 GET: field-level validation, visibility, and input/output translation"
              + " expressions for one field. No design lock is required. Empty lists mean none."
              + " Summary strings match ContentTypeDetail field rows. Jackson root wrap is"
              + " ContentTypeFieldRuleExpressions.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(schema = @Schema(implementation = ContentTypeFieldRuleExpressions.class))),
        @ApiResponse(responseCode = "404", description = "Content type or field not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ContentTypeFieldRuleExpressions getFieldRuleExpressions(
      @PathParam("idOrName") String idOrName, @PathParam("fieldName") String fieldName) {
    try {
      ContentTypeFieldRuleExpressions out =
          requireAdaptor().getFieldRuleExpressions(uriInfo.getBaseUri(), idOrName, fieldName);
      if (out == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return out;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @PUT
  @Path("/{idOrName}/fields/{fieldName}/ruleExpressions")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Replace field rule expressions",
      description =
          "CD-05–07 PUT: full replace of field validation, visibility, and input/output"
              + " translation expressions via IPSContentDesignWs.saveContentTypes. Requires a held"
              + " design-session lock (POST .../lock). Does not acquire or release the lock. Empty"
              + " lists clear. Unknown field names are rejected. Conditional variable/value are"
              + " stored as text literals. Apply-when on field validation is not written. Jackson"
              + " root wrap is ContentTypeFieldRuleExpressions.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Replaced (lock is still held)",
            content =
                @Content(schema = @Schema(implementation = ContentTypeFieldRuleExpressions.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing required lists, invalid rule, or unknown field name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Content type not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Design lock not held by the current user"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ContentTypeFieldRuleExpressions replaceFieldRuleExpressions(
      @PathParam("idOrName") String idOrName,
      @PathParam("fieldName") String fieldName,
      ContentTypeFieldRuleExpressions body) {
    if (body == null
        || body.getValidation() == null
        || body.getVisibility() == null
        || body.getInputTranslation() == null
        || body.getOutputTranslation() == null) {
      throw new WebApplicationException(
          "validation, visibility, inputTranslation, and outputTranslation are required", 400);
    }
    try {
      ContentTypeFieldRuleExpressions out =
          requireAdaptor()
              .replaceFieldRuleExpressions(uriInfo.getBaseUri(), idOrName, fieldName, body);
      if (out == null) {
        throw new WebApplicationException("Content type not found: " + idOrName, 404);
      }
      return out;
    } catch (RuntimeException e) {
      throw mapMutationFailure(e);
    } catch (Exception e) {
      throw mapWriteFailure(e);
    }
  }

  private static NamedObjectRefList asNamedObjectRefList(List<NamedObjectRef> items) {
    if (items instanceof NamedObjectRefList list) {
      return list;
    }
    return new NamedObjectRefList(items);
  }

  /**
   * Map adaptor write failures to HTTP status. Lock conflicts are always 409, including {@link
   * ContentTypeDesignLockException} and {@link IllegalStateException} whose message mentions lock.
   */
  static WebApplicationException mapWriteFailure(Exception e) {
    if (e instanceof ContentTypeDesignLockException) {
      return new WebApplicationException(e.getMessage(), 409);
    }
    if (e instanceof IllegalArgumentException) {
      return new WebApplicationException(e.getMessage(), 400);
    }
    if (e instanceof IllegalStateException) {
      String msg = e.getMessage() != null ? e.getMessage() : "Conflict";
      if (msg.toLowerCase().contains("lock")) {
        return new WebApplicationException(msg, 409);
      }
      return new WebApplicationException(e, 500);
    }
    return new WebApplicationException(e, 500);
  }
}
