/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.operations.remote;

import java.io.File;

import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;

import android.util.Log;

import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.network.webdav.WebdavUtils;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.utils.FileUtils;


/**
 * Remote operation performing the rename of a remote file or folder in the ownCloud server.
 * 
 * @author David A. Velasco
 * @author masensio
 */
public class RenameRemoteFileOperation extends RemoteOperation {

	private static final String TAG = RenameRemoteFileOperation.class.getSimpleName();

	private static final int RENAME_READ_TIMEOUT = 10000;
	private static final int RENAME_CONNECTION_TIMEOUT = 5000;

    private String mOldName;
    private String mOldRemotePath;
    private String mNewName;
    private String mNewRemotePath;
    
    
    /**
     * Constructor
     * 
     * @param oldName			Old name of the file.
     * @param oldRemotePath		Old remote path of the file. 
     * @param newName			New name to set as the name of file.
     * @param isFolder			'true' for folder and 'false' for files
     */
	public RenameRemoteFileOperation(String oldName, String oldRemotePath, String newName, boolean isFolder) {
		mOldName = oldName;
		mOldRemotePath = oldRemotePath;
		mNewName = newName;
		
        String parent = (new File(mOldRemotePath)).getParent();
        parent = (parent.endsWith(FileUtils.PATH_SEPARATOR)) ? parent : parent + FileUtils.PATH_SEPARATOR; 
        mNewRemotePath =  parent + mNewName;
        if (isFolder) {
            mNewRemotePath += FileUtils.PATH_SEPARATOR;
        }
	}

	 /**
     * Performs the rename operation.
     * 
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
		
		LocalMoveMethod move = null;
        
        boolean noInvalidChars = FileUtils.isValidPath(mNewRemotePath);
        
        if (noInvalidChars) {
        try {
        	
            if (mNewName.equals(mOldName)) {
                return new RemoteOperationResult(ResultCode.OK);
            }
        
            
            // check if a file with the new name already exists
            if (client.existsFile(mNewRemotePath)) {
            	return new RemoteOperationResult(ResultCode.INVALID_OVERWRITE);
            }
            
            move = new LocalMoveMethod( client.getWebdavUri() + WebdavUtils.encodePath(mOldRemotePath),
            		client.getWebdavUri() + WebdavUtils.encodePath(mNewRemotePath));
            int status = client.executeMethod(move, RENAME_READ_TIMEOUT, RENAME_CONNECTION_TIMEOUT);
            
            move.getResponseBodyAsString(); // exhaust response, although not interesting
            result = new RemoteOperationResult(move.succeeded(), status, move.getResponseHeaders());
            Log.i(TAG, "Rename " + mOldRemotePath + " to " + mNewRemotePath + ": " + result.getLogMessage());
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Rename " + mOldRemotePath + " to " + ((mNewRemotePath==null) ? mNewName : mNewRemotePath) + ": " + result.getLogMessage(), e);
            
        } finally {
            if (move != null)
                move.releaseConnection();
        }
        } else {
        	result = new RemoteOperationResult(ResultCode.INVALID_CHARACTER_IN_NAME);
        }
        	
        return result;
	}
	
	/**
	 * Move operation
	 * 
	 */
    private class LocalMoveMethod extends DavMethodBase {

        public LocalMoveMethod(String uri, String dest) {
            super(uri);
            addRequestHeader(new org.apache.commons.httpclient.Header("Destination", dest));
        }

        @Override
        public String getName() {
            return "MOVE";
        }

        @Override
        protected boolean isSuccess(int status) {
            return status == 201 || status == 204;
        }
            
    }

}
