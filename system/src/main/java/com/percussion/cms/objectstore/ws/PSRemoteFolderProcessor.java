package com.percussion.cms.objectstore.ws;

import com.percussion.cms.objectstore.PSKey;
import com.percussion.util.IPSRemoteRequester;

/** Minimal stub to satisfy compile for remote folder processor type. */
public class PSRemoteFolderProcessor {

  public static final String PURGE_FOLDER_OPERATION = "purgeFolderOp";
  public static final String PURGE_FOLDER_RESPONSE = "purgeFolderRes";
  public static final String PURGE_FOLDER_REQUEST = "purgeFolderReq";
  public static final String FOLDER_ID_EL = "folderId";

  public PSRemoteFolderProcessor() {
    // no-op
  }

  public PSRemoteFolderProcessor(IPSRemoteRequester requester) {
    // no-op constructor to satisfy callers that pass a requester
  }

  public void delete(PSKey key, java.util.List<?> list) {
    // no-op stub for compilation
  }
}
