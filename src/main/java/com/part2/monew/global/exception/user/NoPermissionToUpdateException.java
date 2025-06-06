package com.part2.monew.global.exception.user;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class NoPermissionToUpdateException extends BusinessException {

  public NoPermissionToUpdateException() {
    super(ErrorCode.NO_PERMISSION_TO_UPDATE, ErrorCode.NO_PERMISSION_TO_UPDATE.getMessage());
  }

  public NoPermissionToUpdateException(String detailMessage) {
    super(ErrorCode.NO_PERMISSION_TO_UPDATE, detailMessage);
  }
}
