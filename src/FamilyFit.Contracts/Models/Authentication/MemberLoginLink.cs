using System.Security.Claims;
using System.Text.Json.Serialization;
using ChannelMediator;

namespace FamilyFit.Contracts.Models.Authentication;

public sealed class MemberLoginLink
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid MemberId { get; set; }
    [JsonIgnore] public string TokenHash { get; set; } = "";
    [JsonIgnore] public string? BrowserSecretHash { get; set; }
    public DateTimeOffset ExpiresAtUtc { get; set; }
    public DateTimeOffset? UsedAtUtc { get; set; }
}
public sealed record MemberLoginLinkCreated(Guid MemberId, string Nickname, string Url, DateTimeOffset ExpiresAtUtc);
public sealed record MemberLoginPreview(Guid MemberId, string Nickname, DateTimeOffset ExpiresAtUtc);
public sealed record BrowserChallengeInput(string LinkSecret);
