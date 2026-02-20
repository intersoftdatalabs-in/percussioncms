/*
 * Minimal compatibility shim for AWS SDK TransferManagerBuilder used by legacy code.
 * Provides the builder API used in our code and returns the simple TransferManager shim.
 */
package com.amazonaws.services.s3.transfer;

import com.amazonaws.services.s3.AmazonS3;

public class TransferManagerBuilder {
  private AmazonS3 s3Client;

  public static TransferManagerBuilder standard() {
    return new TransferManagerBuilder();
  }

  public TransferManagerBuilder withS3Client(AmazonS3 s3Client) {
    this.s3Client = s3Client;
    return this;
  }

  public TransferManager build() {
    return new TransferManager(s3Client);
  }
}
