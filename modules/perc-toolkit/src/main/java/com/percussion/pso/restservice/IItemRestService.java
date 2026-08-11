/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.pso.restservice;

// REFACTORED: CP-JAVA11
import com.percussion.pso.restservice.model.Item;
import com.percussion.pso.restservice.model.Items;
import com.percussion.pso.restservice.model.results.PagedResult;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

/**
 * REST contract for creating, reading, and updating PSO content items.
 */
@Path("/Content/")
public interface IItemRestService {
  /**
   * Method updateItem.
   *
   * @param item Item
   * @return Item
   */
  public Item updateItem(Item item);

  /**
   * updateItem operation.
   *
   * @param item the item
   * @param updateOnly the update only
   * @return the result
   */
  @POST
  @Path("/")
  @Consumes("text/xml")
  public Item updateItem(
      Item item, @QueryParam("updateOnly") @DefaultValue("false") boolean updateOnly);

  /**
   * Method getItem.
   *
   * @param id int
   * @return Item
   */
  @GET
  @Path("{id}")
  public Item getItem(@PathParam("id") int id);

  /**
   * Method getItemRev.
   *
   * @param id int
   * @param rev int
   * @return Item
   */
  @GET
  @Path("{id}/{rev}")
  public Item getItemRev(@PathParam("id") int id, @PathParam("rev") int rev);

  /**
   * Method updateItem.
   *
   * @param id int
   * @param item Item
   * @return Item
   */
  @POST
  @Path("{id}")
  @Consumes("text/xml")
  public Item updateItem(int id, Item item);

  /**
   * Method purgeItem.
   *
   * @param id int
   * @return Item
   */
  @DELETE
  @Path("{id}")
  public Item purgeItem(@PathParam("id") int id);

  /**
   * PurgeAllFolderContent operation.
   *
   * @param target the target
   * @return the result
   */
  @DELETE
  @Path("/PurgeFolder/{target:.*}")
  public Response PurgeAllFolderContent(@PathParam("target") String target);

  /**
   * Method updateItem.
   *
   * @param templateName String
   * @param body String
   * @param debug boolean
   * @return Items
   * @param param the param
   */
  @POST
  @Path("import/{template}")
  public Items updateItem(
      @PathParam("template") String templateName,
      String body,
      @QueryParam("debug") boolean debug,
      @QueryParam("param") String param);

  /**
   * Method getFolders.
   *
   * @param n Integer
   * @return PagedResult
   */
  @GET
  @Path("/Folders/")
  public PagedResult getFolders(@QueryParam("n") Integer n);

  /**
   * Method getItems.
   *
   * @param n Integer
   * @return PagedResult
   */
  @GET
  @Path("/AllContent/")
  public PagedResult getItems(@QueryParam("n") Integer n);

  /**
   * Method getItems.
   *
   * @param path String
   * @param n Integer
   * @return PagedResult
   */
  @GET
  // On upgrade use @Path("/Sites/{search:.*}") remove limited
  // @Path(value="/Sites/{search}", limited=false)
  /**
   * Returns the items.
   *
   * @param path the path
   * @param n the n
   * @return the result
   */
  @Path("/Sites/{search:.*}")
  public PagedResult getItems(@PathParam("search") String path, @QueryParam("n") Integer n);

  /**
   * Returns the type items.
   *
   * @param type the type
   * @param n the n
   * @return the result
   */
  @Path("/Type/{typename}")
  public PagedResult getTypeItems(@PathParam("typename") String type, @QueryParam("n") Integer n);

  // REFACTORED: CP-JAVA11

  /**
   * Returns the file.
   *
   * @param id the id
   * @param revision the revision
   * @param field the field
   * @return the result
   */
  @GET
  @Path("{id}/{rev}/field/{fieldname}")
  @Produces("*/*")
  public Response getFile(
  /**
   * Returns the file.
   *
   * @param id the id
   * @param field the field
   * @return the result
   */
      @PathParam("id") int id,
      @PathParam("rev") int revision,
      @PathParam("fieldname") String field);

  @GET
  @Path("{id}/field/{fieldname}")
  @Produces("*/*")
  public Response getFile(@PathParam("id") int id, @PathParam("fieldname") String field);

  /**
   * updateItems operation.
   *
   * @param debug the debug
   * @param content_type the content type
   * @return the result
   */
  @GET
  @Path("/importfeeds/")
  public Response updateItems(
      @QueryParam("debug") boolean debug, @QueryParam("content_type") String content_type);

  /***
   * Given a content id for a Feed Definition will import the specified feed.
   *
   * @param debug the debug
   * @param contentId the content id
   * @param folderId the folder id
   *
   * @return the result
   */
  @GET
  @Path("/importfeed/")
  public Response updateItem(
      @QueryParam("debug") boolean debug,
      @QueryParam("sys_contentid") int contentId,
      @QueryParam("sys_folderid") int folderId);

  /***
   * Finds an item by a key field.
   * @return null or the Item.
   * @param value the value
   * @param keyfield the keyfield
   * @param contextRoot the context root
   */
  @GET
  @Path("/find/v/{value}/k/{keyfield}/p/{contextRoot}/")
  public Item findByKeyField(
      @PathParam("value") String value,
      @PathParam("keyfield") String keyfield,
      @PathParam("contextRoot") String contextRoot);
}
