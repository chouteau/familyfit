using System.Security.Claims;
using ChannelMediator;
namespace FamilyFit.Contracts.Models.Exercises;
public static class ExerciseExtensions
{
    public static Task<Outcome<Exercise>> CreateExercise(this IMediator mediator, ClaimsPrincipal user, ExerciseInput input, CancellationToken cancellationToken = default)
        => mediator.Send(new CreateExerciseRequest(user, input), cancellationToken);
}
