using ChannelMediator;
using System.Security.Claims;
namespace FamilyFit.Contracts.Models.Authentication;
public sealed record CloseBrowserSessionRequest(ClaimsPrincipal User) : IRequest<Outcome<bool>>, IAuthorizedRequest, IHumanOnlyRequest { public bool AdminOnly => false; }
