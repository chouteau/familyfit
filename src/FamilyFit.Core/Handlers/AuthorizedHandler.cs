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
            var result = await Execute(request, actor, cancellationToken);
            if (result.Success && request is IChangesFamily) await mediator.Publish(new FamilyChangedNotification(), cancellationToken);
            return result;
        }
        catch (DomainException ex) { return Outcome<TResponse>.Fail(ex.Message, ex.Status); }
        catch (DbUpdateConcurrencyException) { return Outcome<TResponse>.Fail("Ces informations ont changé. Recharge la page avant de réessayer.", 409); }
    }
    protected abstract Task<Outcome<TResponse>> Execute(TRequest request, Actor actor, CancellationToken ct);
}
