using ChannelMediator;
namespace FamilyFit.Contracts.Models.Authentication;
public sealed record GetBootstrapStatusRequest : IRequest<Outcome<BootstrapStatus>>;
