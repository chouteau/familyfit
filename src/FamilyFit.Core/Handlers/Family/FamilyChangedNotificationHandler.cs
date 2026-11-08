using ChannelMediator;
using FamilyFit.Contracts.Models.Family;
using FamilyFit.Core.Services;

namespace FamilyFit.Core.Handlers.Family;

public sealed class FamilyChangedNotificationHandler(FamilyUpdates updates) : INotificationHandler<FamilyChangedNotification>
{
    public Task Handle(FamilyChangedNotification notification, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        updates.Notify();
        return Task.CompletedTask;
    }
}
