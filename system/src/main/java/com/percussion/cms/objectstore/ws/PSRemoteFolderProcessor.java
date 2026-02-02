package com.percussion.cms.objectstore.ws;

import com.percussion.cms.objectstore.PSKey;
import com.percussion.util.IPSRemoteRequester;

/** Minimal stub to satisfy compile for remote folder processor type. */
public class PSRemoteFolderProcessor {

  public static final String PURGE_FOLDER_OPERATION = "purgeFolderOp";
  public static final String PURGE_FOLDER_RESPONSE = "purgeFolderRes";
  public static final String PURGE_FOLDER_REQUEST = "purgeFolderReq";
  public static final String FOLDER_ID_EL = "folderId";

  // Additional constants used by PSFolderHandler
  public static final String CREATE_FOLDER_REQUEST = "createFolderReq";
  public static final String OPEN_FOLDER_REQUEST = "openFolderReq";
  public static final String OPEN_FOLDER_OPERATION = "openFolderOp";
  public static final String DELETE_FOLDER_REQUEST = "deleteFolderReq";
  public static final String DELETE_FOLDER_OPERATION = "deleteFolderOp";
  public static final String ADD_FOLDERCHILDREN_REQUEST = "addFolderChildrenReq";
  public static final String TARGET_PARENT_ID_EL = "targetParentId";
  public static final String ADD_FOLDERCHILDREN_OPERATION = "addFolderChildrenOp";
  public static final String COPY_FOLDERCHILDREN_REQUEST = "copyFolderChildrenReq";
  public static final String CLONE_SITEFOLDER_REQUEST = "cloneSiteFolderReq";
  public static final String COPY_FOLDERCHILDREN_OPERATION = "copyFolderChildrenOp";
  public static final String GET_FOLDERCHILDREN_REQUEST = "getFolderChildrenReq";
  public static final String GET_FOLDERCHILDREN_OPERATION = "getFolderChildrenOp";
  public static final String GET_FOLDERCOMMUNITIES_REQUEST = "getFolderCommunitiesReq";
  public static final String GET_FOLDERCOMMUNITIES_OPERATION = "getFolderCommunitiesOp";
  public static final String MOVE_FOLDERCHILDREN_REQUEST = "moveFolderChildrenReq";
  public static final String SOURCE_PARENT_ID_EL = "sourceParentId";
  public static final String FORCE = "force";
  public static final String MOVE_FOLDERCHILDREN_OPERATION = "moveFolderChildrenOp";
  public static final String COPY_FOLDERSECURITY_REQUEST = "copyFolderSecurityReq";
  public static final String COPY_FOLDERSECURITY_OPERATION = "copyFolderSecurityOp";
  public static final String SOURCE_FOLDER_ID_EL = "sourceFolderId";
  public static final String TARGET_FOLDER_ID_EL = "targetFolderId";
  public static final String REMOVE_FOLDERCHILDREN_REQUEST = "removeFolderChildrenReq";
  public static final String PARENT_ID_EL = "parentId";
  public static final String REMOVE_FOLDERCHILDREN_OPERATION = "removeFolderChildrenOp";
  public static final String CHILD_ID_EL = "childId";
  public static final String FOLDER_PATHS_EL = "folderPaths";
  public static final String PATH_EL = "path";
  public static final String GET_PARENTFOLDER_REQUEST = "getParentFolderReq";
  public static final String GET_PARENTFOLDER_OPERATION = "getParentFolderOp";
  public static final String GET_DESCENDENTSLOCATORS_REQUEST = "getDescendentsLocatorsReq";
  public static final String GET_DESCENDENTSLOCATORS_OPERATION = "getDescendentsLocatorsOp";
  public static final String GET_DESCENDENTSLOCATORS_WITHOUTFILTER_REQUEST =
      "getDescendentsLocatorsWithoutFilterReq";
  public static final String GET_DESCENDENTSLOCATORS_WITHOUTFILTER_OPERATION =
      "getDescendentsLocatorsWithoutFilterOp";
  public static final String GET_FOLDER_PATH_REQUEST = "getFolderPathReq";
  public static final String GET_FOLDER_PATH_OPERATION = "getFolderPathOp";
  public static final String UPDATE_FOLDER_REQUEST = "updateFolderReq";
  public static final String UPDATE_FOLDER_OPERATION = "updateFolderOp";
  public static final String NULL_RESULT = "nullResult";
  public static final String GET_SUMMARYBYPATH_REQUEST = "getSummaryByPathReq";
  public static final String CHILD_IDS_EL = "childIds";
  public static final String RECURSIVE_EL = "recursive";

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
