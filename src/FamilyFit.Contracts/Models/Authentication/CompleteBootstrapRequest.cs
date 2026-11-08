using ChannelMediator;
namespace FamilyFit.Contracts.Models.Authentication;
public sealed record CompleteBootstrapRequest(BootstrapInput Input) : IRequest<Outcome<BootstrapGrant>>;
