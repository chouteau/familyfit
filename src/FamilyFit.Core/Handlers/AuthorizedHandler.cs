using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Family;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers;

public abstract class AuthorizedHandler<TRequest, TResponse>(AuthService auth, IMediator mediator)
    : IRequestHandler<TRequest, Outcome<TResponse>> where TRequest : IRequest<Outcome<TResponse>>, IAuthorizedRequest
{
    protected AuthService Auth => auth;
    protected IMediator Dispatcher => mediator;
    public async Task<Outcome<TResponse>> Handle(TRequest request, CancellationToken cancellationToken)
    {
        try
        {
            var actor = await auth.RequireActorAsync(request.User, request.AdminOnly, cancellationToken);
            if (actor.AgentId != null && request is IHumanOnlyRequest) return Outcome<TResponse>.Fail("Cette opération exige un accès humain par le mobile ou le site.", 403);
            if (actor.AgentId != null && !actor.CanWrite && request is IChangesFamily)
                return Outcome<TResponse>.Fail("Ce serveur MCP est configuré en lecture seule.", 403);
            var result = await Execute(request, actor, cancellationToken);
            if (result.Success && request is IChangesFamily) await mediator.Publish(new FamilyChangedNotification(), cancellationToken);
            return result;
        }
        catch (DomainException ex) { return Outcome<TResponse>.Fail(ex.Message, ex.Status); }
        catch (DbUpdateConcurrencyException) { return Outcome<TResponse>.Fail("Ces informations ont changé. Recharge la page avant de réessayer.", 409); }
    }
    protected abstract Task<Outcome<TResponse>> Execute(TRequest request, Actor actor, CancellationToken ct);
}
