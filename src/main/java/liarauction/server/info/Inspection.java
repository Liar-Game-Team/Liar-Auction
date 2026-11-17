package liarauction.server.info;

import liarauction.common.Grade;

/**
 * 한 플레이어가 받는 감정 정보.
 *
 * @param official true면 감정서, false면 소문
 * @param claimedGrade 감정 정보가 주장하는 등급 (틀릴 수 있음)
 * @param text 클라이언트에 보낼 문장
 */
public record Inspection(boolean official, Grade claimedGrade, String text) {
}
