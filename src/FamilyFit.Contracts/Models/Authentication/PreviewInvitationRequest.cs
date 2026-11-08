using ChannelMediator;
namespace FamilyFit.Contracts.Models.Authentication;
public sealed record PreviewInvitationRequest(string Code) : IRequest<Outcome<InvitationPreview>>;
