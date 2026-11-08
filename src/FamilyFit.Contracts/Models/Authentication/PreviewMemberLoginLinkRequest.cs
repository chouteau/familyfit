using ChannelMediator;

namespace FamilyFit.Contracts.Models.Authentication;

public sealed record PreviewMemberLoginLinkRequest(string? LinkSecret, string? BrowserSecret) : IRequest<Outcome<MemberLoginPreview?>>;
