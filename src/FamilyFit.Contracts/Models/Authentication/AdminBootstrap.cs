using ChannelMediator;

namespace FamilyFit.Contracts.Models.Authentication;

public sealed class AdminBootstrap
{
    public int Id { get; set; } = 1;
    public Guid MemberId { get; set; }
    public DateTimeOffset CompletedAtUtc { get; set; }
}
public sealed record BootstrapStatus(bool Available, bool NeedsProfile);
public sealed record BootstrapInput(string Code, string? Nickname = null, int Age = 18, double WeightKg = 70);
public sealed record BootstrapGrant(Member Member, BrowserSession Session);
