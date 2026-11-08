using System.Security.Claims;

namespace FamilyFit.Contracts;

public sealed record Outcome<T>(T? Value, string? Error = null, int Status = 200)
{
    public bool Success => Error == null;
    public static Outcome<T> Ok(T value) => new(value);
    public static Outcome<T> Fail(string error, int status = 400) => new(default, error, status);
}

public interface IAuthorizedRequest
{
    ClaimsPrincipal User { get; }
    bool AdminOnly { get; }
}
public interface IChangesFamily;
public sealed record BrowserChallengeWithSecret(ChallengeGrant Grant, string BrowserSecret);
public sealed record BrowserLoginResult(string State, Models.BrowserSession? Session, Models.Member? Member = null);
public sealed record InvitationPreview(Guid MemberId, string Nickname, DateTimeOffset ExpiresAtUtc);
public interface IHumanOnlyRequest;
