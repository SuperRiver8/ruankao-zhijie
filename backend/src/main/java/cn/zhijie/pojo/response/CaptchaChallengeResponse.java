package cn.zhijie.pojo.response;

public record CaptchaChallengeResponse(
    String challengeId,
    String background,
    String piece,
    int width,
    int height,
    int pieceWidth,
    int pieceHeight,
    int pieceY,
    int expiresIn
) {}
