using System.Security.Claims;
using ChannelMediator;

namespace FamilyFit.Contracts.Models.Agents;

public sealed record AgentQuery(string Resource, Guid? MemberId = null, Guid? Id = null, DateOnly? From = null, DateOnly? To = null, int Skip = 0, int Take = 50, string? Search = null, string? EquipmentId = null, string? State = null);
public sealed record AgentPage(string Resource, object Items, int Skip, int Take, bool HasMore);
public sealed record GetAgentDataRequest(ClaimsPrincipal User, AgentQuery Query) : IRequest<Outcome<object>>, IAuthorizedRequest
{
    public bool AdminOnly => false;
}
